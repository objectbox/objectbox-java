package io.objectbox;

import org.greenrobot.essentials.collections.LongHashMap;
import org.greenrobot.essentials.collections.MultimapSet;
import org.greenrobot.essentials.collections.MultimapSet.SetType;
import org.greenrobot.essentials.io.IoUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

import io.objectbox.BoxStoreBuilder.EntityClasses;
import io.objectbox.annotation.apihint.Beta;
import io.objectbox.annotation.apihint.Internal;
import io.objectbox.converter.PropertyConverter;
import io.objectbox.exception.DbSchemaException;
import io.objectbox.internal.CrashReportLogger;

@Beta
public class BoxStore implements Closeable {
    static {
        String libname = "objectbox";
        // For Android, os.name is also "Linux", so we need an extra check
        if (!System.getProperty("java.vendor").contains("Android")) {
            String osName = System.getProperty("os.name");
            String sunArch = System.getProperty("sun.arch.data.model");
            if (osName.contains("Windows")) {
                libname += "-windows" + ("32".equals(sunArch) ? "-x86" : "-x64");
                checkUnpackLib(libname + ".dll");
            } else if (osName.contains("Linux")) {
                libname += "-linux" + ("32".equals(sunArch) ? "-x86" : "-x64");
                checkUnpackLib("lib" + libname + ".so");
            }
        }
        System.loadLibrary(libname);
    }

    private static void checkUnpackLib(String filename) {
        String path = "/native/" + filename;
        URL resource = BoxStore.class.getResource(path);
        if (resource == null) {
            System.err.println("Not available in classpath: " + path);
        } else {
            File file = new File(filename);
            try {
                URLConnection urlConnection = resource.openConnection();
                int length = urlConnection.getContentLength();
                long lastModified = urlConnection.getLastModified();
                if (!file.exists() || file.length() != length || file.lastModified() != lastModified) {
                    InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                    try {
                        OutputStream out = new BufferedOutputStream(new FileOutputStream(file));
                        try {
                            IoUtils.copyAllBytes(in, out);
                        } finally {
                            IoUtils.safeClose(out);
                        }
                    } finally {
                        IoUtils.safeClose(in);
                    }
                    if (lastModified > 0) {
                        file.setLastModified(lastModified);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }
    }

    private static BoxStore defaultStore;

    /**
     * Convenience singleton instance which gets set up using {@link BoxStoreBuilder#buildDefault()}.
     * <p>
     * Note: for better testability, you can usually avoid singletons by storing
     * a {@link BoxStore} instance in some application scope object and pass it along.
     */
    public static synchronized BoxStore getDefault() {
        if (defaultStore == null) {
            throw new IllegalStateException("Please call buildDefault() before calling this method");
        }
        return defaultStore;
    }

    static synchronized void setDefault(BoxStore store) {
        if (defaultStore != null) {
            throw new IllegalStateException("Default store was already built before. ");
        }
        defaultStore = store;
    }

    /**
     * Clears the convenience instance.
     * <p>
     * Note: This is usually not required (for testability, please see the comment of     * {@link #getDefault()}).
     *
     * @return true if a default store was available before
     */
    public static synchronized boolean clearDefaultStore() {
        boolean existedBefore = defaultStore != null;
        defaultStore = null;
        return existedBefore;
    }

    public static native String getVersionNative();

    /**
     * Diagnostics: If this method crashes on a device, please send us the logcat output.
     */
    public static native void testUnalignedMemoryAccess();

    public static native void setCrashReportLogger(CrashReportLogger crashReportLogger);

    static native long nativeCreate(String directory, long maxDbSizeInKByte, byte[] model);

    static native void nativeDelete(long store);

    static native void nativeDropAllData(long store);

    static native long nativeBeginTx(long store);

    static native long nativeBeginReadTx(long store);

    static native long nativeCreateIndex(long store, String name, int entityId, int propertyId);


    /** @return entity ID */
    // TODO only use ids once we have them in Java
    static native int nativeRegisterEntityClass(long store, String entityName, Class entityClass);

    // TODO only use ids once we have them in Java
    static native void nativeRegisterCustomType(long store, int entityId, int propertyId, String propertyName,
                                                Class<? extends PropertyConverter> converterClass, Class customType);

    static native String nativeDiagnose(long store);

    static native int nativeCleanStaleReadTransactions(long store);

    public static String getVersion() {
        return "0.9.8-2017-02-12";
    }

    private final File directory;
    private final long handle;
    private final Map<Class, String> entityNameByClass;
    private final Map<Class, Class<Cursor>> entityCursorClassByClass;
    private final Map<Class, Integer> entityTypeIdByClass;
    private final LongHashMap<Class> classByEntityTypeId;
    private final int[] allEntityTypeIds;
    private final MultimapSet<Integer, ObjectClassListener> listenersByEntityTypeId;
    private final Map<Class, Box> boxes = new ConcurrentHashMap<>();
    private final Set<Transaction> transactions = Collections.newSetFromMap(new WeakHashMap<Transaction, Boolean>());

    /** Set when running inside TX */
    final ThreadLocal<Transaction> activeTx = new ThreadLocal<>();

    private boolean closed;

    Object txCommitCountLock = new Object();
    // Not atomic because it is read most of the time
    volatile int commitCount;

    BoxStore(BoxStoreBuilder builder) {
        this.directory = builder.directory;
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                throw new RuntimeException("Could not create directory: " +
                        directory.getAbsolutePath());
            }
        }
        if (!directory.isDirectory()) {
            throw new RuntimeException("Is not a directory: " + directory.getAbsolutePath());
        }
        handle = nativeCreate(directory.getAbsolutePath(), builder.maxSizeInKByte, builder.model);
        entityNameByClass = new HashMap<>();
        entityCursorClassByClass = new HashMap<>();
        entityTypeIdByClass = new HashMap<>();
        classByEntityTypeId = new LongHashMap<>();
        listenersByEntityTypeId = MultimapSet.create(SetType.THREAD_SAFE);

        for (EntityClasses entity : builder.entityClasses) {
            try {
                entityNameByClass.put(entity.entityClass, entity.entityName);
                entityCursorClassByClass.put(entity.entityClass, entity.cursorClass);
                int entityId = nativeRegisterEntityClass(handle, entity.entityName, entity.entityClass);
                entityTypeIdByClass.put(entity.entityClass, entityId);
                classByEntityTypeId.put(entityId, entity.entityClass);
                for (Property property : entity.properties.getAllProperties()) {
                    if (property.customType != null) {
                        if (property.converterClass == null) {
                            throw new RuntimeException("No converter class for custom type");
                        }
                        nativeRegisterCustomType(handle, entityId, 0, property.dbName, property.converterClass,
                                property.customType);
                    }
                }
            } catch (RuntimeException e) {
                throw new RuntimeException("Could not setup up entity " + entity.entityClass, e);
            }
        }
        int size = classByEntityTypeId.size();
        allEntityTypeIds = new int[size];
        long[] entityIdsLong = classByEntityTypeId.keys();
        for (int i = 0; i < size; i++) {
            allEntityTypeIds[i] = (int) entityIdsLong[i];
        }
    }

    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

    private void checkOpen() {
        if (closed) {
            throw new IllegalStateException("Store is closed");
        }
    }

    String getEntityName(Class entityClass) {
        return entityNameByClass.get(entityClass);
    }

    Integer getEntityId(Class entityClass) {
        return entityTypeIdByClass.get(entityClass);
    }

    @Internal
    public int getEntityIdOrThrow(Class entityClass) {
        Integer id = entityTypeIdByClass.get(entityClass);
        if (id == null) {
            throw new DbSchemaException("No entity registered for " + entityClass);
        }
        return id;
    }

    <T> Class<Cursor<T>> getEntityCursorClass(Class<T> entityClass) {
        return (Class) entityCursorClassByClass.get(entityClass);
    }

    public Transaction beginTx() {
        checkOpen();
        // Because write TXs are typically not cached, initialCommitCount is not as relevant than for read TXs.
        int initialCommitCount = commitCount;
        long nativeTx = nativeBeginTx(handle);
        Transaction tx = new Transaction(this, nativeTx, initialCommitCount);
        synchronized (transactions) {
            transactions.add(tx);
        }
        return tx;
    }

    /**
     * Begins a transaction for read access only. Note: there may be only one read transaction per thread.
     */
    public Transaction beginReadTx() {
        checkOpen();
        // initialCommitCount should be acquired before starting the tx. In race conditions, there is a chance the
        // commitCount is already outdated. That's OK because it only gives a false positive for an TX being obsolete.
        // In contrast, a false negative would make a TX falsely not considered obsolete, and thus readers would not be
        // updated resulting in querying obsolete data until another commit is done.
        // TODO add multithreaded test for this
        int initialCommitCount = commitCount;
        long nativeTx = nativeBeginReadTx(handle);
        Transaction tx = new Transaction(this, nativeTx, initialCommitCount);
        synchronized (transactions) {
            transactions.add(tx);
        }
        return tx;
    }

    public boolean isClosed() {
        return closed;
    }

    public synchronized void close() {
        if (!closed) {
            closed = true;
            List<Transaction> transactionsToClose;
            synchronized (transactions) {
                transactionsToClose = new ArrayList<>(this.transactions);
            }
            for (Transaction t : transactionsToClose) {
                t.close();
            }
            nativeDelete(handle);
        }
    }

    public boolean deleteAllFiles() {
        if (!closed) {
            throw new IllegalStateException("Store must be closed");
        }
        return deleteAllFiles(directory);
    }

    public static boolean deleteAllFiles(File objectStoreDirectory) {
        boolean ok = true;
        if (objectStoreDirectory != null && objectStoreDirectory.exists()) {
            File[] files = objectStoreDirectory.listFiles();
            if (files != null) {
                for (File file : files) {
                    ok &= file.delete();
                }
            } else {
                ok = false;
            }
            ok &= objectStoreDirectory.delete();
        }
        return ok;
    }

    public void unregisterTransaction(Transaction transaction) {
        synchronized (transactions) {
            transactions.remove(transaction);
        }
    }

    public void dropAllData() {
        nativeDropAllData(handle);
    }

    void txCommitted(Transaction tx, int[] entityTypeIdsAffected) {
        // Only one write TX at a time, but there is a chance two writers race after commit: thus synchronize
        synchronized (txCommitCountLock) {
            commitCount++; // Overflow is OK because we check for equality
        }

        for (Box box : boxes.values()) {
            box.txCommitted(tx);
        }

        if (entityTypeIdsAffected != null) {
            for (int entityTypeId : entityTypeIdsAffected) {
                Collection<ObjectClassListener> listeners = listenersByEntityTypeId.get(entityTypeId);
                if (listeners != null) {
                    Class objectClass = classByEntityTypeId.get(entityTypeId);
                    if (objectClass == null) {
                        throw new IllegalStateException("Untracked entity type ID: " + entityTypeId);
                    }
                    for (ObjectClassListener listener : listeners) {
                        listener.handleChanges(objectClass);
                    }
                }
            }
        }
    }

    public <T> Box<T> boxFor(Class<T> entityClass) {
        Box box = boxes.get(entityClass);
        if (box == null) {
            if (!entityNameByClass.containsKey(entityClass)) {
                throw new IllegalArgumentException(entityClass +
                        " is not a known entity. Please add it and trigger generation again.");
            }
            // Ensure a box is created just once
            synchronized (boxes) {
                box = boxes.get(entityClass);
                if (box == null) {
                    box = new Box<>(this, entityClass);
                    boxes.put(entityClass, box);
                }
            }
        }
        return box;
    }

    public void runInTx(Runnable runnable) {
        Transaction tx = this.activeTx.get();
        // Only if not already set, allowing to call it recursively with first (outer) TX
        if (tx == null) {
            tx = beginTx();
            activeTx.set(tx);
            try {
                runnable.run();
                tx.commit();
            } finally {
                activeTx.remove();
                tx.close();
            }
        } else {
            if (tx.isReadOnly()) {
                throw new IllegalStateException("Cannot start a transaction while a read only transaction is active");
            }
            runnable.run();
        }
    }

    public void runInReadTx(Runnable runnable) {
        Transaction tx = this.activeTx.get();
        // Only if not already set, allowing to call it recursively with first (outer) TX
        if (tx == null) {
            tx = beginReadTx();
            activeTx.set(tx);
            try {
                runnable.run();
            } finally {
                activeTx.remove();
                tx.close();
            }
        } else {
            runnable.run();
        }
    }

    public <R> R callInTx(Callable<R> callable) throws Exception {
        Transaction tx = this.activeTx.get();
        // Only if not already set, allowing to call it recursively with first (outer) TX
        if (tx == null) {
            tx = beginTx();
            activeTx.set(tx);
            try {
                R result = callable.call();
                tx.commit();
                return result;
            } finally {
                activeTx.remove();
                tx.close();
            }
        } else {
            if (tx.isReadOnly()) {
                throw new IllegalStateException("Cannot start a transaction while a read only transaction is active");
            }
            return callable.call();
        }
    }

    /**
     * Runs the given Runnable as a transaction in a separate thread.
     * Once the transaction completes the given callback is called (callback may be null).
     */
    public void runInTxAsync(final Runnable runnable, final TxCallback<Void> callback) {
        new Thread() {
            @Override
            public void run() {
                try {
                    runInTx(runnable);
                    if (callback != null) {
                        callback.txFinished(null, null);
                    }
                } catch (Throwable failure) {
                    if (callback != null) {
                        callback.txFinished(null, failure);
                    }
                }
            }
        }.start();
    }

    /**
     * Runs the given Runnable as a transaction in a separate thread.
     * Once the transaction completes the given callback is called (callback may be null).
     */
    public <R> void callInTxAsync(final Callable<R> callable, final TxCallback<R> callback) {
        new Thread() {
            @Override
            public void run() {
                try {
                    R result = callInTx(callable);
                    if (callback != null) {
                        callback.txFinished(result, null);
                    }
                } catch (Throwable failure) {
                    if (callback != null) {
                        callback.txFinished(null, failure);
                    }
                }
            }
        }.start();
    }

    public String diagnose() {
        return nativeDiagnose(handle);
    }

    public int cleanStaleReadTransactions() {
        return nativeCleanStaleReadTransactions(handle);
    }

    @Internal
    long internalHandle() {
        return handle;
    }

    public void addObjectClassListener(ObjectClassListener objectClassListener) {
        for (int entityTypeId : allEntityTypeIds) {
            listenersByEntityTypeId.putElement(entityTypeId, objectClassListener);
        }
    }

    public void addObjectClassListener(ObjectClassListener objectClassListener, Class objectClass) {
        Integer entityTypeId = entityTypeIdByClass.get(objectClass);
        if (entityTypeId == null) {
            throw new IllegalArgumentException("Not a registered object class: " + objectClass);
        }
        listenersByEntityTypeId.putElement(entityTypeId, objectClassListener);
    }

    /**
     * Removes the given objectClassListener from all object classes it added itself to earlier.
     */
    public void removeObjectClassListener(ObjectClassListener objectClassListener) {
        for (int entityTypeId : allEntityTypeIds) {
            listenersByEntityTypeId.removeElement(entityTypeId, objectClassListener);
        }
    }

}
