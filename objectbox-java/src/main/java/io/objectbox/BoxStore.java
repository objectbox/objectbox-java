package io.objectbox;

import org.greenrobot.essentials.collections.LongHashMap;

import java.io.Closeable;
import java.io.File;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.annotation.concurrent.ThreadSafe;

import io.objectbox.annotation.apihint.Beta;
import io.objectbox.annotation.apihint.Internal;
import io.objectbox.converter.PropertyConverter;
import io.objectbox.exception.DbSchemaException;
import io.objectbox.internal.CrashReportLogger;
import io.objectbox.internal.NativeLibraryLoader;
import io.objectbox.internal.ObjectBoxThreadPool;
import io.objectbox.reactive.DataObserver;
import io.objectbox.reactive.DataPublisher;
import io.objectbox.reactive.SubscriptionBuilder;

/**
 * Represents an ObjectBox database and gives you {@link Box}es to get and put Objects of a specific type
 * (see {@link #boxFor(Class)}).
 */
@Beta
@ThreadSafe
public class BoxStore implements Closeable {

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

    static native long nativeCreate(String directory, long maxDbSizeInKByte, int maxReaders, byte[] model);

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
        return "0.9.12-2017-05-03";
    }

    private final File directory;
    private final long handle;
    private final Map<Class, String> dbNameByClass = new HashMap<>();
    private final Map<Class, Integer> entityTypeIdByClass = new HashMap<>();
    private final Map<Class, EntityInfo> propertiesByClass = new HashMap<>();
    private final LongHashMap<Class> classByEntityTypeId = new LongHashMap<>();
    private final int[] allEntityTypeIds;
    private final Map<Class, Box> boxes = new ConcurrentHashMap<>();
    private final Set<Transaction> transactions = Collections.newSetFromMap(new WeakHashMap<Transaction, Boolean>());
    private final ExecutorService threadPool = new ObjectBoxThreadPool(this);
    private final ObjectClassPublisher objectClassPublisher;
    final boolean debugTx;

    /** Set when running inside TX */
    final ThreadLocal<Transaction> activeTx = new ThreadLocal<>();

    private boolean closed;

    final Object txCommitCountLock = new Object();

    // Not atomic because it is read most of the time
    volatile int commitCount;

    BoxStore(BoxStoreBuilder builder) {
        NativeLibraryLoader.ensureLoaded();

        this.directory = builder.directory;
        if (directory.exists()) {
            if (!directory.isDirectory()) {
                throw new RuntimeException("Is not a directory: " + directory.getAbsolutePath());
            }
        } else if (!directory.mkdirs()) {
            throw new RuntimeException("Could not create directory: " + directory.getAbsolutePath());
        }
        handle = nativeCreate(directory.getAbsolutePath(), builder.maxSizeInKByte, 0, builder.model);
        debugTx = builder.debugTransactions;

        for (EntityInfo entityInfo : builder.entityInfoList) {
            try {
                dbNameByClass.put(entityInfo.getEntityClass(), entityInfo.getDbName());
                int entityId = nativeRegisterEntityClass(handle, entityInfo.getDbName(), entityInfo.getEntityClass());
                entityTypeIdByClass.put(entityInfo.getEntityClass(), entityId);
                classByEntityTypeId.put(entityId, entityInfo.getEntityClass());
                propertiesByClass.put(entityInfo.getEntityClass(), entityInfo);
                for (Property property : entityInfo.getAllProperties()) {
                    if (property.customType != null) {
                        if (property.converterClass == null) {
                            throw new RuntimeException("No converter class for custom type of " + property);
                        }
                        nativeRegisterCustomType(handle, entityId, 0, property.dbName, property.converterClass,
                                property.customType);
                    }
                }
            } catch (RuntimeException e) {
                throw new RuntimeException("Could not setup up entity " + entityInfo.getEntityClass(), e);
            }
        }
        int size = classByEntityTypeId.size();
        allEntityTypeIds = new int[size];
        long[] entityIdsLong = classByEntityTypeId.keys();
        for (int i = 0; i < size; i++) {
            allEntityTypeIds[i] = (int) entityIdsLong[i];
        }

        objectClassPublisher = new ObjectClassPublisher(this);
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

    String getDbName(Class entityClass) {
        return dbNameByClass.get(entityClass);
    }

    Integer getEntityTypeId(Class entityClass) {
        return entityTypeIdByClass.get(entityClass);
    }

    @Internal
    public int getEntityTypeIdOrThrow(Class entityClass) {
        Integer id = entityTypeIdByClass.get(entityClass);
        if (id == null) {
            throw new DbSchemaException("No entity registered for " + entityClass);
        }
        return id;
    }

    public Collection<Class> getAllEntityClasses() {
        return dbNameByClass.keySet();
    }

    @Internal
    int[] getAllEntityTypeIds() {
        return allEntityTypeIds;
    }

    @Internal
    Class getEntityClassOrThrow(int entityTypeId) {
        Class clazz = classByEntityTypeId.get(entityTypeId);
        if (clazz == null) {
            throw new DbSchemaException("No entity registered for type ID " + entityTypeId);
        }
        return clazz;
    }

    @Internal
    EntityInfo getEntityInfo(Class entityClass) {
        return propertiesByClass.get(entityClass);
    }

    /**
     * Internal, low level method: use {@link #runInTx(Runnable)} instead.
     */
    @Internal
    public Transaction beginTx() {
        checkOpen();
        // Because write TXs are typically not cached, initialCommitCount is not as relevant than for read TXs.
        int initialCommitCount = commitCount;
        if (debugTx) {
            System.out.println("Begin TX with commit count " + initialCommitCount);
        }
        long nativeTx = nativeBeginTx(handle);
        Transaction tx = new Transaction(this, nativeTx, initialCommitCount);
        synchronized (transactions) {
            transactions.add(tx);
        }
        return tx;
    }

    /**
     * Internal, low level method: use {@link #runInReadTx(Runnable)} instead.
     * Begins a transaction for read access only. Note: there may be only one read transaction per thread.
     */
    @Internal
    public Transaction beginReadTx() {
        checkOpen();
        // initialCommitCount should be acquired before starting the tx. In race conditions, there is a chance the
        // commitCount is already outdated. That's OK because it only gives a false positive for an TX being obsolete.
        // In contrast, a false negative would make a TX falsely not considered obsolete, and thus readers would not be
        // updated resulting in querying obsolete data until another commit is done.
        // TODO add multithreaded test for this
        int initialCommitCount = commitCount;
        if (debugTx) {
            System.out.println("Begin read TX with commit count " + initialCommitCount);
        }
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

            // When running the full unit test suite, we had 100+ threads before, hope this helps:
            threadPool.shutdown();
            checkThreadTermination();
        }
    }

    /** dump thread stacks if pool does not terminate promptly. */
    private void checkThreadTermination() {
        try {
            if (!threadPool.awaitTermination(1, TimeUnit.SECONDS)) {
                int activeCount = Thread.activeCount();
                System.err.println("Thread pool not terminated in time; printing stack traces...");
                Thread[] threads = new Thread[activeCount + 2];
                int count = Thread.enumerate(threads);
                for (int i = 0; i < count; i++) {
                    System.err.println("Thread: " + threads[i].getName());
                    threads[i].dumpStack();
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
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

    @Internal
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
            if (debugTx) {
                System.out.println("TX committed, new with commit count " + commitCount);
            }
        }

        for (Box box : boxes.values()) {
            box.txCommitted(tx);
        }

        if (entityTypeIdsAffected != null) {
            objectClassPublisher.publish(entityTypeIdsAffected);
        }
    }

    /**
     * Returns a Box for the given type. Objects are put into (and get from) their individual Box.
     */
    public <T> Box<T> boxFor(Class<T> entityClass) {
        Box box = boxes.get(entityClass);
        if (box == null) {
            if (!dbNameByClass.containsKey(entityClass)) {
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

    /**
     * Runs the given runnable inside a transaction.
     * <p>
     * Efficiency notes: it is advised to run multiple puts in a transaction because each commit requires an expensive
     * disk synchronization.
     */
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

    /**
     * Runs the given runnable inside a read(-only) transaction. Multiple read transactions can occur at the same time.
     * This allows multiple read operations (gets) using a single consistent state of data.
     * Also, for a high number of read operations (thousands, e.g. in loops),
     * it is advised to run them in a single read transaction for efficiency reasons.
     */
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

                // TODO That's rather a quick fix, replace with a more general solution
                // (that could maybe be a TX listener with abort callback?)
                for (Box box : boxes.values()) {
                    box.readTxFinished(tx);
                }

                tx.close();
            }
        } else {
            runnable.run();
        }
    }

    /**
     * Like {@link #runInTx(Runnable)}, but allows returning a value and throwing an exception.
     */
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
     * <p>
     * See also {@link #runInTx(Runnable)}.
     */
    public void runInTxAsync(final Runnable runnable, final TxCallback<Void> callback) {
        threadPool.submit(new Runnable() {
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
        });
    }

    /**
     * Runs the given Runnable as a transaction in a separate thread.
     * Once the transaction completes the given callback is called (callback may be null).
     * <p>
     * * See also {@link #callInTx(Callable)}.
     */
    public <R> void callInTxAsync(final Callable<R> callable, final TxCallback<R> callback) {
        threadPool.submit(new Runnable() {
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
        });
    }

    public String diagnose() {
        return nativeDiagnose(handle);
    }

    public int cleanStaleReadTransactions() {
        return nativeCleanStaleReadTransactions(handle);
    }

    /**
     * Call this method from a thread that is about to be shutdown or likely not to use ObjectBox anymore:
     * it frees any cached resources tied to the calling thread (e.g. readers). This method calls
     * {@link Box#closeThreadResources()} for all initiated boxes ({@link #boxFor(Class)}).
     */
    public void closeThreadResources() {
        for (Box box : boxes.values()) {
            box.closeThreadResources();
        }
        // activeTx is cleaned up in finally blocks, so do not free them here
    }

    @Internal
    long internalHandle() {
        return handle;
    }

    /**
     * A {@link io.objectbox.reactive.DataObserver} can be subscribed to data changes using the returned builder.
     * The observer is supplied via {@link SubscriptionBuilder#observer(DataObserver)} and will be notified once a
     * transaction is committed and will receive changes to any object class.
     * <p>
     * Threading notes:
     * All observers are notified from one separate thread (pooled). Observers are not notified in parallel.
     * The notification order is the same as the subscription order, although this may not always be guaranteed in
     * the future.
     * <p>
     * Note that failed or aborted transaction do not trigger observers.
     */
    public SubscriptionBuilder<Class> subscribe() {
        return new SubscriptionBuilder<>(objectClassPublisher, null, threadPool);
    }

    /**
     * Like {@link #subscribe()}, but wires the supplied @{@link io.objectbox.reactive.DataObserver} only to the given
     * object class for notifications.
     */
    public <T> SubscriptionBuilder<Class<T>> subscribe(Class<T> forClass) {
        return new SubscriptionBuilder<>((DataPublisher) objectClassPublisher, forClass, threadPool);
    }

    @Internal
    public Future internalScheduleThread(Runnable runnable) {
        return threadPool.submit(runnable);
    }

    @Internal
    public ExecutorService internalThreadPool() {
        return threadPool;
    }

}
