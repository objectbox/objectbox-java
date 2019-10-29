/*
 * Copyright 2017-2019 ObjectBox Ltd. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.objectbox;

import org.greenrobot.essentials.collections.LongHashMap;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import io.objectbox.annotation.apihint.Experimental;
import io.objectbox.annotation.apihint.Internal;
import io.objectbox.converter.PropertyConverter;
import io.objectbox.exception.DbException;
import io.objectbox.exception.DbExceptionListener;
import io.objectbox.exception.DbSchemaException;
import io.objectbox.internal.NativeLibraryLoader;
import io.objectbox.internal.ObjectBoxThreadPool;
import io.objectbox.reactive.DataObserver;
import io.objectbox.reactive.DataPublisher;
import io.objectbox.reactive.SubscriptionBuilder;

/**
 * Represents an ObjectBox database and gives you {@link Box}es to get and put Objects of a specific type
 * (see {@link #boxFor(Class)}).
 */
@SuppressWarnings({"unused", "UnusedReturnValue", "SameParameterValue", "WeakerAccess"})
@ThreadSafe
public class BoxStore implements Closeable {

    /** On Android used for native library loading. */
    @Nullable public static Object context;
    @Nullable public static Object relinker;

    /** Change so ReLinker will update native library when using workaround loading. */
    public static final String JNI_VERSION = "2.4.1";

    private static final String VERSION = "2.4.1-2019-10-29";
    private static BoxStore defaultStore;

    /** Currently used DB dirs with values from {@link #getCanonicalPath(File)}. */
    private static final Set<String> openFiles = new HashSet<>();
    private static volatile Thread openFilesCheckerThread;

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
     * Note: This is usually not required (for testability, please see the comment of {@link #getDefault()}).
     *
     * @return true if a default store was available before
     */
    public static synchronized boolean clearDefaultStore() {
        boolean existedBefore = defaultStore != null;
        defaultStore = null;
        return existedBefore;
    }

    /** Gets the Version of ObjectBox Java. */
    public static String getVersion() {
        return VERSION;
    }

    static native String nativeGetVersion();

    /** Gets the Version of ObjectBox Core. */
    public static String getVersionNative() {
        NativeLibraryLoader.ensureLoaded();
        return nativeGetVersion();
    }

    /**
     * Diagnostics: If this method crashes on a device, please send us the logcat output.
     */
    public static native void testUnalignedMemoryAccess();

    static native long nativeCreate(String directory, long maxDbSizeInKByte, int maxReaders, byte[] model);

    static native void nativeDelete(long store);

    static native void nativeDropAllData(long store);

    static native long nativeBeginTx(long store);

    static native long nativeBeginReadTx(long store);

    /** @return entity ID */
    // TODO only use ids once we have them in Java
    static native int nativeRegisterEntityClass(long store, String entityName, Class entityClass);

    // TODO only use ids once we have them in Java
    static native void nativeRegisterCustomType(long store, int entityId, int propertyId, String propertyName,
                                                Class<? extends PropertyConverter> converterClass, Class customType);

    static native String nativeDiagnose(long store);

    static native int nativeCleanStaleReadTransactions(long store);

    static native void nativeSetDbExceptionListener(long store, DbExceptionListener dbExceptionListener);

    static native void nativeSetDebugFlags(long store, int debugFlags);

    static native String nativeStartObjectBrowser(long store, @Nullable String urlPath, int port);

    static native boolean nativeIsObjectBrowserAvailable();

    public static boolean isObjectBrowserAvailable() {
        NativeLibraryLoader.ensureLoaded();
        return nativeIsObjectBrowserAvailable();
    }

    native long nativePanicModeRemoveAllObjects(long store, int entityId);

    private final File directory;
    private final String canonicalPath;
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
    final boolean debugTxRead;
    final boolean debugTxWrite;
    final boolean debugRelations;

    /** Set when running inside TX */
    final ThreadLocal<Transaction> activeTx = new ThreadLocal<>();

    private boolean closed;

    final Object txCommitCountLock = new Object();

    // Not atomic because it is read most of the time
    volatile int commitCount;

    private int objectBrowserPort;

    private final int queryAttempts;

    private final TxCallback failedReadTxAttemptCallback;

    BoxStore(BoxStoreBuilder builder) {
        context = builder.context;
        relinker = builder.relinker;
        NativeLibraryLoader.ensureLoaded();

        directory = builder.directory;
        canonicalPath = getCanonicalPath(directory);
        verifyNotAlreadyOpen(canonicalPath);

        handle = nativeCreate(canonicalPath, builder.maxSizeInKByte, builder.maxReaders, builder.model);
        int debugFlags = builder.debugFlags;
        if (debugFlags != 0) {
            nativeSetDebugFlags(handle, debugFlags);
            debugTxRead = (debugFlags & DebugFlags.LOG_TRANSACTIONS_READ) != 0;
            debugTxWrite = (debugFlags & DebugFlags.LOG_TRANSACTIONS_WRITE) != 0;
        } else {
            debugTxRead = debugTxWrite = false;
        }
        debugRelations = builder.debugRelations;

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

        failedReadTxAttemptCallback = builder.failedReadTxAttemptCallback;
        queryAttempts = builder.queryAttempts < 1 ? 1 : builder.queryAttempts;
    }

    static String getCanonicalPath(File directory) {
        if (directory.exists()) {
            if (!directory.isDirectory()) {
                throw new DbException("Is not a directory: " + directory.getAbsolutePath());
            }
        } else if (!directory.mkdirs()) {
            throw new DbException("Could not create directory: " + directory.getAbsolutePath());
        }
        try {
            return directory.getCanonicalPath();
        } catch (IOException e) {
            throw new DbException("Could not verify dir", e);
        }
    }

    static void verifyNotAlreadyOpen(String canonicalPath) {
        synchronized (openFiles) {
            isFileOpen(canonicalPath); // for retries
            if (!openFiles.add(canonicalPath)) {
                throw new DbException("Another BoxStore is still open for this directory: " + canonicalPath +
                        ". Hint: for most apps it's recommended to keep a BoxStore for the app's life time.");
            }
        }
    }

    /** Also retries up to 500ms to improve GC race condition situation. */
    static boolean isFileOpen(final String canonicalPath) {
        synchronized (openFiles) {
            if (!openFiles.contains(canonicalPath)) return false;
        }
        if(openFilesCheckerThread == null || !openFilesCheckerThread.isAlive()) {
            // Use a thread to avoid finalizers that block us
            openFilesCheckerThread = new Thread() {
                @Override
                public void run() {
                    isFileOpenSync(canonicalPath, true);
                    openFilesCheckerThread = null; // Clean ref to itself
                }
            };
            openFilesCheckerThread.setDaemon(true);
            openFilesCheckerThread.start();
            try {
                openFilesCheckerThread.join(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            // Waiting for finalizers are blocking; only do that in the thread ^
            return isFileOpenSync(canonicalPath, false);
        }
        synchronized (openFiles) {
            return openFiles.contains(canonicalPath);
        }
    }

    static boolean isFileOpenSync(String canonicalPath, boolean runFinalization) {
        synchronized (openFiles) {
            int tries = 0;
            while (tries < 5 && openFiles.contains(canonicalPath)) {
                tries++;
                System.gc();
                if (runFinalization && tries > 1) System.runFinalization();
                System.gc();
                if (runFinalization && tries > 1) System.runFinalization();
                try {
                    openFiles.wait(100);
                } catch (InterruptedException e) {
                    // Ignore
                }
            }
            return openFiles.contains(canonicalPath);
        }
    }

    /**
     * Explicitly call {@link #close()} instead to avoid expensive finalization.
     */
    @SuppressWarnings("deprecation") // finalize()
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
        if (debugTxWrite) {
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
        if (debugTxRead) {
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

    /**
     * Closes the BoxStore and frees associated resources.
     * This method is useful for unit tests;
     * most real applications should open a BoxStore once and keep it open until the app dies.
     * <p>
     * WARNING:
     * This is a somewhat delicate thing to do if you have threads running that may potentially still use the BoxStore.
     * This results in undefined behavior, including the possibility of crashing.
     */
    public void close() {
        boolean oldClosedState;
        synchronized (this) {
            oldClosedState = closed;
            if (!closed) {
                // Closeable recommendation: mark as closed before any code that might throw.
                closed = true;
                List<Transaction> transactionsToClose;
                synchronized (transactions) {
                    transactionsToClose = new ArrayList<>(this.transactions);
                }
                for (Transaction t : transactionsToClose) {
                    t.close();
                }
                if (handle != 0) { // failed before native handle was created?
                    nativeDelete(handle);
                }

                // When running the full unit test suite, we had 100+ threads before, hope this helps:
                threadPool.shutdown();
                checkThreadTermination();
            }
        }
        if (!oldClosedState) {
            synchronized (openFiles) {
                openFiles.remove(canonicalPath);
                openFiles.notifyAll();
            }
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
                    Thread.dumpStack();
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Danger zone! This will delete all data (files) of this BoxStore!
     * You must call {@link #close()} before and read the docs of that method carefully!
     * <p>
     * A safer alternative: use the static {@link #deleteAllFiles(File)} method before opening the BoxStore.
     *
     * @return true if the directory 1) was deleted successfully OR 2) did not exist in the first place.
     * Note: If false is returned, any number of files may have been deleted before the failure happened.
     */
    public boolean deleteAllFiles() {
        if (!closed) {
            throw new IllegalStateException("Store must be closed");
        }
        return deleteAllFiles(directory);
    }

    /**
     * Danger zone! This will delete all files in the given directory!
     * <p>
     * No {@link BoxStore} may be alive using the given directory.
     * <p>
     * If you did not use a custom name with BoxStoreBuilder, you can pass "new File({@link
     * BoxStoreBuilder#DEFAULT_NAME})".
     *
     * @param objectStoreDirectory directory to be deleted; this is the value you previously provided to {@link
     *                             BoxStoreBuilder#directory(File)}
     * @return true if the directory 1) was deleted successfully OR 2) did not exist in the first place.
     * Note: If false is returned, any number of files may have been deleted before the failure happened.
     * @throws IllegalStateException if the given directory is still used by a open {@link BoxStore}.
     */
    public static boolean deleteAllFiles(File objectStoreDirectory) {
        if (!objectStoreDirectory.exists()) {
            return true;
        }
        if (isFileOpen(getCanonicalPath(objectStoreDirectory))) {
            throw new IllegalStateException("Cannot delete files: store is still open");
        }

        File[] files = objectStoreDirectory.listFiles();
        if (files == null) {
            return false;
        }
        for (File file : files) {
            if (!file.delete()) {
                // OK if concurrently deleted. Fail fast otherwise.
                if (file.exists()) {
                    return false;
                }
            }
        }
        return objectStoreDirectory.delete();
    }

    /**
     * Danger zone! This will delete all files in the given directory!
     * <p>
     * No {@link BoxStore} may be alive using the given name.
     * <p>
     * If you did not use a custom name with BoxStoreBuilder, you can pass "new File({@link
     * BoxStoreBuilder#DEFAULT_NAME})".
     *
     * @param androidContext     provide an Android Context like Application or Service
     * @param customDbNameOrNull use null for default name, or the name you previously provided to {@link
     *                           BoxStoreBuilder#name(String)}.
     * @return true if the directory 1) was deleted successfully OR 2) did not exist in the first place.
     * Note: If false is returned, any number of files may have been deleted before the failure happened.
     * @throws IllegalStateException if the given name is still used by a open {@link BoxStore}.
     */
    public static boolean deleteAllFiles(Object androidContext, @Nullable String customDbNameOrNull) {
        File dbDir = BoxStoreBuilder.getAndroidDbDir(androidContext, customDbNameOrNull);
        return deleteAllFiles(dbDir);
    }

    /**
     * Danger zone! This will delete all files in the given directory!
     * <p>
     * No {@link BoxStore} may be alive using the given directory.
     * <p>
     * If you did not use a custom name with BoxStoreBuilder, you can pass "new File({@link
     * BoxStoreBuilder#DEFAULT_NAME})".
     *
     * @param baseDirectoryOrNull use null for no base dir, or the value you previously provided to {@link
     *                            BoxStoreBuilder#baseDirectory(File)}
     * @param customDbNameOrNull  use null for default name, or the name you previously provided to {@link
     *                            BoxStoreBuilder#name(String)}.
     * @return true if the directory 1) was deleted successfully OR 2) did not exist in the first place.
     * Note: If false is returned, any number of files may have been deleted before the failure happened.
     * @throws IllegalStateException if the given directory (+name) is still used by a open {@link BoxStore}.
     */
    public static boolean deleteAllFiles(@Nullable File baseDirectoryOrNull, @Nullable String customDbNameOrNull) {
        File dbDir = BoxStoreBuilder.getDbDir(baseDirectoryOrNull, customDbNameOrNull);
        return deleteAllFiles(dbDir);
    }

    @Internal
    public void unregisterTransaction(Transaction transaction) {
        synchronized (transactions) {
            transactions.remove(transaction);
        }
    }

    // TODO not implemented on native side; rename to "nukeData" (?)
    void dropAllData() {
        nativeDropAllData(handle);
    }

    void txCommitted(Transaction tx, @Nullable int[] entityTypeIdsAffected) {
        // Only one write TX at a time, but there is a chance two writers race after commit: thus synchronize
        synchronized (txCommitCountLock) {
            commitCount++; // Overflow is OK because we check for equality
            if (debugTxWrite) {
                System.out.println("TX committed. New commit count: " + commitCount + ", entity types affected: " +
                        (entityTypeIdsAffected != null ? entityTypeIdsAffected.length : 0));
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
     * <p>
     * Creates a Box only once and then always returns the cached instance.
     */
    @SuppressWarnings("unchecked")
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
        Transaction tx = activeTx.get();
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
        Transaction tx = activeTx.get();
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
     * Calls {@link #callInReadTx(Callable)} and retries in case a DbException is thrown.
     * If the given amount of attempts is reached, the last DbException will be thrown.
     * Experimental: API might change.
     */
    @Experimental
    public <T> T callInReadTxWithRetry(Callable<T> callable, int attempts, int initialBackOffInMs, boolean logAndHeal) {
        if (attempts == 1) {
            return callInReadTx(callable);
        } else if (attempts < 1) {
            throw new IllegalArgumentException("Illegal value of attempts: " + attempts);
        }
        long backoffInMs = initialBackOffInMs;
        DbException lastException = null;
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                return callInReadTx(callable);
            } catch (DbException e) {
                lastException = e;

                String diagnose = diagnose();
                String message = attempt + " of " + attempts + " attempts of calling a read TX failed:";
                if (logAndHeal) {
                    System.err.println(message);
                    e.printStackTrace();
                    System.err.println(diagnose);
                    System.err.flush();

                    System.gc();
                    System.runFinalization();
                    cleanStaleReadTransactions();
                }
                if (failedReadTxAttemptCallback != null) {
                    //noinspection unchecked
                    failedReadTxAttemptCallback.txFinished(null, new DbException(message + " \n" + diagnose, e));
                }
                try {
                    Thread.sleep(backoffInMs);
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                    throw lastException;
                }
                backoffInMs *= 2;
            }
        }
        throw lastException;
    }

    /**
     * Calls the given callable inside a read(-only) transaction. Multiple read transactions can occur at the same time.
     * This allows multiple read operations (gets) using a single consistent state of data.
     * Also, for a high number of read operations (thousands, e.g. in loops),
     * it is advised to run them in a single read transaction for efficiency reasons.
     * Note that an exception thrown by the given Callable will be wrapped in a RuntimeException, if the exception is
     * not a RuntimeException itself.
     */
    public <T> T callInReadTx(Callable<T> callable) {
        Transaction tx = activeTx.get();
        // Only if not already set, allowing to call it recursively with first (outer) TX
        if (tx == null) {
            tx = beginReadTx();
            activeTx.set(tx);
            try {
                return callable.call();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException("Callable threw exception", e);
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
            try {
                return callable.call();
            } catch (Exception e) {
                throw new RuntimeException("Callable threw exception", e);
            }
        }
    }

    /**
     * Like {@link #runInTx(Runnable)}, but allows returning a value and throwing an exception.
     */
    public <R> R callInTx(Callable<R> callable) throws Exception {
        Transaction tx = activeTx.get();
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
     * Like {@link #callInTx(Callable)}, but throws no Exception.
     * Any Exception thrown in the Callable is wrapped in a RuntimeException.
     */
    public <R> R callInTxNoException(Callable<R> callable) {
        try {
            return callInTx(callable);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Runs the given Runnable as a transaction in a separate thread.
     * Once the transaction completes the given callback is called (callback may be null).
     * <p>
     * See also {@link #runInTx(Runnable)}.
     */
    public void runInTxAsync(final Runnable runnable, @Nullable final TxCallback<Void> callback) {
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
    public <R> void callInTxAsync(final Callable<R> callable, @Nullable final TxCallback<R> callback) {
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

    /**
     * Gives info that can be useful for debugging.
     *
     * @return String that is typically logged by the application.
     */
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

    @Experimental
    @Nullable
    public String startObjectBrowser() {
        verifyObjectBrowserNotRunning();
        final int basePort = 8090;
        for (int port = basePort; port < basePort + 10; port++) {
            try {
                String url = startObjectBrowser(port);
                if (url != null) {
                    return url;
                }
            } catch (DbException e) {
                if (e.getMessage() == null || !e.getMessage().contains("port")) {
                    throw e;
                }
            }
        }
        return null;
    }

    @Experimental
    @Nullable
    public String startObjectBrowser(int port) {
        verifyObjectBrowserNotRunning();
        String url = nativeStartObjectBrowser(handle, null, port);
        if (url != null) {
            objectBrowserPort = port;
        }
        return url;
    }

    @Experimental
    public int getObjectBrowserPort() {
        return objectBrowserPort;
    }

    private void verifyObjectBrowserNotRunning() {
        if (objectBrowserPort != 0) {
            throw new DbException("ObjectBrowser is already running at port " + objectBrowserPort);
        }
    }

    /**
     * The given listener will be called when an exception is thrown.
     * This for example allows a central error handling, e.g. a special logging for DB related exceptions.
     */
    public void setDbExceptionListener(DbExceptionListener dbExceptionListener) {
        nativeSetDbExceptionListener(handle, dbExceptionListener);
    }

    /**
     * Like {@link #subscribe()}, but wires the supplied @{@link io.objectbox.reactive.DataObserver} only to the given
     * object class for notifications.
     */
    @SuppressWarnings("unchecked")
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

    @Internal
    public boolean isDebugRelations() {
        return debugRelations;
    }

    @Internal
    public int internalQueryAttempts() {
        return queryAttempts;
    }

    @Internal
    public TxCallback internalFailedReadTxAttemptCallback() {
        return failedReadTxAttemptCallback;
    }

    void setDebugFlags(int debugFlags) {
        nativeSetDebugFlags(handle, debugFlags);
    }

    long panicModeRemoveAllObjects(int entityId) {
        return nativePanicModeRemoveAllObjects(handle, entityId);
    }

    /**
     * If you want to use the same ObjectBox store using the C API, e.g. via JNI, this gives the required pointer,
     * which you have to pass on to obx_store_wrap().
     * The procedure is like this:<br>
     * 1) you create a BoxStore on the Java side<br>
     * 2) you call this method to get the native store pointer<br>
     * 3) you pass the native store pointer to your native code (e.g. via JNI)<br>
     * 4) your native code calls obx_store_wrap() with the native store pointer to get a OBX_store pointer<br>
     * 5) Using the OBX_store pointer, you can use the C API.
     *
     * Note: Once you {@link #close()} this BoxStore, do not use it from the C API.
     */
    public long getNativeStore() {
        if (closed) {
            throw new IllegalStateException("Store must still be open");
        }
        return handle;
    }

}
