/*
 * Copyright 2017-2025 ObjectBox Ltd.
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
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
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
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import io.objectbox.annotation.apihint.Beta;
import io.objectbox.annotation.apihint.Experimental;
import io.objectbox.annotation.apihint.Internal;
import io.objectbox.config.DebugFlags;
import io.objectbox.config.FlatStoreOptions;
import io.objectbox.converter.PropertyConverter;
import io.objectbox.exception.DbException;
import io.objectbox.exception.DbExceptionListener;
import io.objectbox.exception.DbSchemaException;
import io.objectbox.internal.Feature;
import io.objectbox.internal.NativeLibraryLoader;
import io.objectbox.internal.ObjectBoxThreadPool;
import io.objectbox.reactive.DataObserver;
import io.objectbox.reactive.DataPublisher;
import io.objectbox.reactive.SubscriptionBuilder;
import io.objectbox.sync.SyncClient;

/**
 * An ObjectBox database that provides {@link Box Boxes} to put and get objects of specific entity classes
 * (see {@link #boxFor(Class)}). To get an instance of this class use {@code MyObjectBox.builder()}.
 */
@SuppressWarnings({"unused", "UnusedReturnValue", "SameParameterValue", "WeakerAccess"})
@ThreadSafe
public class BoxStore implements Closeable {

    /** On Android used for native library loading. */
    @Nullable private static Object context;
    @Nullable private static Object relinker;

    /** Prefix supplied with database directory to signal a file-less and in-memory database should be used. */
    public static final String IN_MEMORY_PREFIX = "memory:";

    /**
     * ReLinker uses this as a suffix for the extracted shared library file. If different, it will update it. Should be
     * unique to avoid conflicts.
     */
    public static final String JNI_VERSION = "5.1.1-2026-02-16";

    /**
     * The ObjectBox database version this Java library is known to work with.
     * <p>
     * This should be a version number followed by a date (MAJOR.MINOR.PATCH-YYYY-MM-DD).
     * <p>
     * This is used (currently only in tests) to make sure a database library has a compatible JNI API by checking the
     * version number matches exactly and the date is the same or newer.
     */
    private static final String VERSION = "5.1.1-2026-02-16";

    private static final String OBJECTBOX_PACKAGE_NAME = "objectbox";
    private static BoxStore defaultStore;

    /** Currently used DB dirs with values from {@link #getCanonicalPath(File)}. */
    private static final Set<String> openFiles = new HashSet<>();
    private static volatile Thread openFilesCheckerThread;

    @Nullable
    @Internal
    public static synchronized Object getContext() {
        return context;
    }

    @Nullable
    @Internal
    public static synchronized Object getRelinker() {
        return relinker;
    }

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

    /**
     * Returns the version of this ObjectBox Java SDK.
     */
    public static String getVersion() {
        return VERSION;
    }

    static native String nativeGetVersion();

    /**
     * Returns the version of the loaded ObjectBox database library.
     */
    public static String getVersionNative() {
        NativeLibraryLoader.ensureLoaded();
        return nativeGetVersion();
    }

    /**
     * @return true if DB files did not exist or were successfully removed,
     * false if DB files exist that could not be removed.
     */
    static native boolean nativeRemoveDbFiles(String directory, boolean removeDir);

    /**
     * Creates a native BoxStore instance with FlatBuffer {@link FlatStoreOptions} {@code options}
     * and a {@link ModelBuilder} {@code model}. Returns the handle of the native store instance.
     */
    static native long nativeCreateWithFlatOptions(byte[] options, byte[] model);

    static native boolean nativeIsReadOnly(long store);

    static native void nativeDelete(long store);

    static native void nativeDropAllData(long store);

    /**
     * A static counter for the alive entity types (entity schema instances); this can be useful to test against leaks.
     * This number depends on the number of currently opened stores; no matter how often stores were closed and
     * (re-)opened. E.g. when stores are regularly opened, but not closed by the user, the number should increase. When
     * all stores are properly closed, this value should be 0.
     */
    @Internal
    static native long nativeGloballyActiveEntityTypes();

    static native long nativeBeginTx(long store);

    static native long nativeBeginReadTx(long store);

    /** @return entity ID */
    // TODO only use ids once we have them in Java
    static native int nativeRegisterEntityClass(long store, String entityName, Class<?> entityClass);

    // TODO only use ids once we have them in Java
    static native void nativeRegisterCustomType(long store, int entityId, int propertyId, String propertyName,
                                                Class<? extends PropertyConverter> converterClass, Class<?> customType);

    static native String nativeDiagnose(long store);

    static native int nativeCleanStaleReadTransactions(long store);

    static native void nativeSetDbExceptionListener(long store, @Nullable DbExceptionListener dbExceptionListener);

    static native void nativeSetDebugFlags(long store, int debugFlags);

    private native String nativeStartObjectBrowser(long store, @Nullable String urlPath, int port);

    private native boolean nativeStopObjectBrowser(long store);

    static native boolean nativeIsObjectBrowserAvailable();

    native long nativeDbSize(long store);

    native long nativeDbSizeOnDisk(long store);

    native long nativeValidate(long store, long pageLimit, boolean checkLeafLevel);

    static native long nativeSysProcMeminfoKb(String key);

    static native long nativeSysProcStatusKb(String key);

    private static native boolean nativeHasFeature(int feature);

    public static boolean hasFeature(Feature feature) {
        try {
            NativeLibraryLoader.ensureLoaded();
            return nativeHasFeature(feature.id);
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Old JNI lib? " + e);  // No stack
            return false;
        }
    }

    public static boolean isObjectBrowserAvailable() {
        return hasFeature(Feature.ADMIN);
    }

    public static boolean isSyncAvailable() {
        return hasFeature(Feature.SYNC);
    }

    public static boolean isSyncServerAvailable() {
        return hasFeature(Feature.SYNC_SERVER);
    }

    native long nativePanicModeRemoveAllObjects(long store, int entityId);

    private final PrintStream errorOutputStream;
    private final File directory;
    private final String canonicalPath;

    /** Reference to the native store. Should probably get through {@link #getNativeStore()} instead. */
    volatile private long handle;
    volatile private boolean nativeStoreDestroyed = false;

    private final Map<Class<?>, String> dbNameByClass = new HashMap<>();
    private final Map<Class<?>, Integer> entityTypeIdByClass = new HashMap<>();
    private final Map<Class<?>, EntityInfo<?>> propertiesByClass = new HashMap<>();
    private final LongHashMap<Class<?>> classByEntityTypeId = new LongHashMap<>();
    private final int[] allEntityTypeIds;
    private final Map<Class<?>, Box<?>> boxes = new ConcurrentHashMap<>();
    private final Set<Transaction> transactions = Collections.newSetFromMap(new WeakHashMap<>());
    private final ExecutorService threadPool = new ObjectBoxThreadPool(this);
    private final ObjectClassPublisher objectClassPublisher;
    final boolean debugTxRead;
    final boolean debugTxWrite;
    final boolean debugRelations;

    /** Set when running inside TX */
    final ThreadLocal<Transaction> activeTx = new ThreadLocal<>();

    // volatile so checkOpen() is more up-to-date (no need for synchronized; it's a race anyway)
    volatile private boolean closed;

    final Object txCommitCountLock = new Object();

    // Not atomic because it is read most of the time
    volatile int commitCount;

    private int objectBrowserPort;

    private final int queryAttempts;

    private final TxCallback<?> failedReadTxAttemptCallback;

    /**
     * Keeps a reference so the library user does not have to.
     */
    @Nullable
    private SyncClient syncClient;

    BoxStore(BoxStoreBuilder builder) {
        context = builder.context;
        relinker = builder.relinker;
        NativeLibraryLoader.ensureLoaded();

        errorOutputStream = builder.errorOutputStream;
        directory = builder.directory;
        canonicalPath = getCanonicalPath(directory);
        verifyNotAlreadyOpen(canonicalPath);

        try {
            handle = nativeCreateWithFlatOptions(builder.buildFlatStoreOptions(canonicalPath), builder.model);
            if (handle == 0) throw new DbException("Could not create native store");

            int debugFlags = builder.debugFlags;
            if (debugFlags != 0) {
                debugTxRead = (debugFlags & DebugFlags.LOG_TRANSACTIONS_READ) != 0;
                debugTxWrite = (debugFlags & DebugFlags.LOG_TRANSACTIONS_WRITE) != 0;
            } else {
                debugTxRead = debugTxWrite = false;
            }
            debugRelations = builder.debugRelations;

            for (EntityInfo<?> entityInfo : builder.entityInfoList) {
                try {
                    dbNameByClass.put(entityInfo.getEntityClass(), entityInfo.getDbName());
                    int entityId = nativeRegisterEntityClass(handle, entityInfo.getDbName(), entityInfo.getEntityClass());
                    entityTypeIdByClass.put(entityInfo.getEntityClass(), entityId);
                    classByEntityTypeId.put(entityId, entityInfo.getEntityClass());
                    propertiesByClass.put(entityInfo.getEntityClass(), entityInfo);
                    for (Property<?> property : entityInfo.getAllProperties()) {
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
            queryAttempts = Math.max(builder.queryAttempts, 1);
        } catch (RuntimeException runtimeException) {
            close();  // Proper clean up, e.g. delete native handle, remove this path from openFiles
            throw runtimeException;
        }
    }

    static String getCanonicalPath(File directory) {
        // Skip directory check if in-memory prefix is used.
        if (directory.getPath().startsWith(IN_MEMORY_PREFIX)) {
            // Just return the path as is (e.g. "memory:data"), safe to use for string-based open check as well.
            return directory.getPath();
        }

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
        // Call isFileOpen before, but without checking the result, to try to close any unreferenced instances where
        // it was forgotten to close them.
        // Only obtain the lock on openFiles afterward as the checker thread created by isFileOpen needs to obtain it to
        // do anything.
        isFileOpen(canonicalPath);
        synchronized (openFiles) {
            if (!openFiles.add(canonicalPath)) {
                throw new DbException("Another BoxStore is still open for this directory (" + canonicalPath +
                        "). Make sure the existing instance is explicitly closed before creating a new one.");
            }
        }
    }

    /**
     * Returns if the given path is in {@link #openFiles}.
     * <p>
     * If it is, (creates and) briefly waits on an existing "checker" thread before checking again and returning the
     * result.
     * <p>
     * The "checker" thread locks {@link #openFiles} while it triggers garbage collection and finalization in this Java
     * Virtual Machine to try to close any unreferenced BoxStore instances. These might exist if it was forgotten to
     * close them explicitly.
     * <p>
     * Note that the finalization mechanism relied upon here is scheduled for removal in future versions of Java and may
     * already be disabled depending on JVM configuration.
     *
     * @see #finalize()
     */
    static boolean isFileOpen(final String canonicalPath) {
        synchronized (openFiles) {
            if (!openFiles.contains(canonicalPath)) return false;
        }
        Thread checkerThread = BoxStore.openFilesCheckerThread;
        if (checkerThread == null || !checkerThread.isAlive()) {
            // Use a thread to avoid finalizers that block us
            checkerThread = new Thread(() -> {
                isFileOpenSync(canonicalPath, true);
                BoxStore.openFilesCheckerThread = null; // Clean ref to itself
            });
            checkerThread.setDaemon(true);

            BoxStore.openFilesCheckerThread = checkerThread;
            checkerThread.start();
            try {
                checkerThread.join(500);
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
     * Using an Android Context and an optional database name, as configured with {@link BoxStoreBuilder#name(String)},
     * checks if the associated database files are in use by a BoxStore instance.
     * <p>
     * Use this to check that database files are not open before copying or deleting them.
     */
    public static boolean isDatabaseOpen(Object context, @Nullable String dbNameOrNull) throws IOException {
        File dbDir = BoxStoreBuilder.getAndroidDbDir(context, dbNameOrNull);
        return isFileOpen(dbDir.getCanonicalPath());
    }

    /**
     * Using an optional base directory, as configured with {@link BoxStoreBuilder#baseDirectory(File)},
     * and an optional database name, as configured with {@link BoxStoreBuilder#name(String)},
     * checks if the associated database files are in use by a BoxStore instance.
     * <p>
     * Use this to check that database files are not open before copying or deleting them.
     */
    public static boolean isDatabaseOpen(@Nullable File baseDirectoryOrNull,
                                         @Nullable String dbNameOrNull) throws IOException {
        File dbDir = BoxStoreBuilder.getDbDir(baseDirectoryOrNull, dbNameOrNull);
        return isFileOpen(dbDir.getCanonicalPath());
    }

    /**
     * Using a directory, as configured with {@link BoxStoreBuilder#directory(File)},
     * checks if the associated database files are in use by a BoxStore instance.
     * <p>
     * Use this to check that database files are not open before copying or deleting them.
     */
    public static boolean isDatabaseOpen(File directory) throws IOException {
        return isFileOpen(directory.getCanonicalPath());
    }

    /**
     * Linux only: extracts a kB value from /proc/meminfo (system wide memory information).
     * A couple of interesting keys (from 'man proc'):
     * - MemTotal: Total usable RAM (i.e., physical RAM minus a few reserved bits and the kernel binary code).
     * - MemFree:  The sum of LowFree+HighFree.
     * - MemAvailable: An estimate of how much memory is available for starting new applications, without swapping.
     *
     * @param key The string identifying the wanted line from /proc/meminfo to extract a Kb value from. E.g. "MemTotal".
     * @return Kb value or 0 on failure
     */
    @Experimental
    public static long sysProcMeminfoKb(String key) {
        NativeLibraryLoader.ensureLoaded();
        return nativeSysProcMeminfoKb(key);
    }

    /**
     * Linux only: extracts a kB value from /proc/self/status (process specific information).
     * A couple of interesting keys (from 'man proc'):
     * - VmPeak: Peak virtual memory size.
     * - VmSize: Virtual memory size.
     * - VmHWM: Peak resident set size ("high water mark").
     * - VmRSS: Resident set size.  Note that the value here is the sum of RssAnon, RssFile, and RssShmem.
     * - RssAnon: Size of resident anonymous memory.  (since Linux 4.5).
     * - RssFile: Size of resident file mappings.  (since Linux 4.5).
     * - RssShmem: Size of resident shared memory (includes System V shared  memory,  mappings  from  tmpfs(5),
     * and shared anonymous mappings).  (since Linux 4.5).
     * - VmData, VmStk, VmExe: Size of data, stack, and text segments.
     * - VmLib: Shared library code size.
     *
     * @param key The string identifying the wanted line from /proc/self/status to extract a Kb value from. E.g. "VmSize".
     * @return Kb value or 0 on failure
     */
    @Experimental
    public static long sysProcStatusKb(String key) {
        NativeLibraryLoader.ensureLoaded();
        return nativeSysProcStatusKb(key);
    }

    /**
     * Get the size of this store. For a disk-based store type, this corresponds to the size on disk, and for the
     * in-memory store type, this is roughly the used memory bytes occupied by the data.
     *
     * @return The size in bytes of the database, or 0 if the file does not exist or some error occurred.
     */
    public long getDbSize() {
        return nativeDbSize(getNativeStore());
    }

    /**
     * The size in bytes occupied by the database on disk (if any).
     *
     * @return The size in bytes of the database on disk, or 0 if the underlying database is in-memory only
     * or the size could not be determined.
     */
    public long getDbSizeOnDisk() {
        return nativeDbSizeOnDisk(getNativeStore());
    }

    /**
     * Calls {@link #close()}.
     * <p>
     * It is strongly recommended to instead explicitly close a Store and not rely on finalization. For example, on
     * Android finalization has a timeout that might be exceeded if closing needs to wait on transactions to finish.
     * <p>
     * Also finalization is scheduled for removal in future versions of Java and may already be disabled depending on
     * JVM configuration (see documentation on super method).
     */
    @SuppressWarnings("deprecation") // finalize()
    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

    /**
     * Verifies this has not been {@link #close() closed}.
     */
    private void checkOpen() {
        if (isClosed()) {
            throw new IllegalStateException("Store is closed");
        }
    }

    String getDbName(Class<?> entityClass) {
        return dbNameByClass.get(entityClass);
    }

    Integer getEntityTypeId(Class<?> entityClass) {
        return entityTypeIdByClass.get(entityClass);
    }

    @Internal
    public int getEntityTypeIdOrThrow(Class<?> entityClass) {
        Integer id = entityTypeIdByClass.get(entityClass);
        if (id == null) {
            throw new DbSchemaException("No entity registered for " + entityClass);
        }
        return id;
    }

    public Collection<Class<?>> getAllEntityClasses() {
        return dbNameByClass.keySet();
    }

    @Internal
    int[] getAllEntityTypeIds() {
        return allEntityTypeIds;
    }

    @Internal
    Class<?> getEntityClassOrThrow(int entityTypeId) {
        Class<?> clazz = classByEntityTypeId.get(entityTypeId);
        if (clazz == null) {
            throw new DbSchemaException("No entity registered for type ID " + entityTypeId);
        }
        return clazz;
    }

    @SuppressWarnings("unchecked") // Casting is easier than writing a custom Map.
    @Internal
    <T> EntityInfo<T> getEntityInfo(Class<T> entityClass) {
        return (EntityInfo<T>) propertiesByClass.get(entityClass);
    }

    /**
     * Internal, low level method: use {@link #runInTx(Runnable)} instead.
     */
    @Internal
    public Transaction beginTx() {
        // Because write TXs are typically not cached, initialCommitCount is not as relevant than for read TXs.
        int initialCommitCount = commitCount;
        if (debugTxWrite) {
            getOutput().println("Begin TX with commit count " + initialCommitCount);
        }
        long nativeTx = nativeBeginTx(getNativeStore());
        if (nativeTx == 0) throw new DbException("Could not create native transaction");

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
        // initialCommitCount should be acquired before starting the tx. In race conditions, there is a chance the
        // commitCount is already outdated. That's OK because it only gives a false positive for an TX being obsolete.
        // In contrast, a false negative would make a TX falsely not considered obsolete, and thus readers would not be
        // updated resulting in querying obsolete data until another commit is done.
        // TODO add multithreaded test for this
        int initialCommitCount = commitCount;
        if (debugTxRead) {
            getOutput().println("Begin read TX with commit count " + initialCommitCount);
        }
        long nativeTx = nativeBeginReadTx(getNativeStore());
        if (nativeTx == 0) throw new DbException("Could not create native read transaction");

        Transaction tx = new Transaction(this, nativeTx, initialCommitCount);
        synchronized (transactions) {
            transactions.add(tx);
        }
        return tx;
    }

    /**
     * If this was {@link #close() closed}.
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * Whether the store was created using read-only mode.
     * If true the schema is not updated and write transactions are not possible.
     */
    public boolean isReadOnly() {
        return nativeIsReadOnly(getNativeStore());
    }

    /**
     * Closes this BoxStore and releases associated resources.
     * <p>
     * Before calling, <b>all database operations must have finished</b> (there are no more active transactions).
     * <p>
     * If that is not the case, the method will briefly wait on any active transactions, but then will forcefully close
     * them to avoid crashes and print warning messages ("Transactions are still active"). If this occurs,
     * analyze your code to make sure all database operations, notably in other threads or data observers,
     * are properly finished.
     */
    public void close() {
        boolean oldClosedState;
        synchronized (this) {
            oldClosedState = closed;
            if (!closed) {
                if (objectBrowserPort != 0) { // not linked natively (yet), so clean up here
                    try {
                        stopObjectBrowser();
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }

                // Closeable recommendation: mark as closed before any code that might throw.
                // Also, before checking on transactions to avoid any new transactions from getting created
                // (due to all Java APIs doing closed checks).
                closed = true;

                // Stop accepting new tasks (async calls, query publishers) on the internal thread pool
                internalThreadPool().shutdown();
                // Give running tasks some time to finish, print warnings if they do not to help callers fix their code
                checkThreadTermination();

                List<Transaction> transactionsToClose;
                synchronized (transactions) {
                    // Give open transactions some time to close (BoxStore.unregisterTransaction() calls notify),
                    // 1000 ms should be long enough for most small operations and short enough to avoid ANRs on Android.
                    if (hasActiveTransaction()) {
                        getOutput().println("Briefly waiting for active transactions before closing the Store...");
                        try {
                            // It is fine to hold a lock on BoxStore.this as well as BoxStore.unregisterTransaction()
                            // only synchronizes on "transactions".
                            //noinspection WaitWhileHoldingTwoLocks
                            transactions.wait(1000);
                        } catch (InterruptedException e) {
                            // If interrupted, continue with releasing native resources
                        }
                        if (hasActiveTransaction()) {
                            getErrorOutput().println("Transactions are still active:"
                                    + " ensure that all database operations are finished before closing the Store!");
                        }
                    }
                    transactionsToClose = new ArrayList<>(this.transactions);
                }
                // Close all transactions, including recycled (not active) ones stored in Box threadLocalReader.
                // It is expected that this prints a warning if a transaction is not owned by the current thread.
                for (Transaction t : transactionsToClose) {
                    t.close();
                }

                long handleToDelete = handle;
                // Make isNativeStoreClosed() return true before actually closing to avoid Transaction.close() crash
                handle = 0;
                if (handleToDelete != 0) { // failed before native handle was created?
                    nativeDelete(handleToDelete);
                    nativeStoreDestroyed = true;
                }
            }
        }
        if (!oldClosedState) {
            synchronized (openFiles) {
                openFiles.remove(canonicalPath);
                openFiles.notifyAll();
            }
        }
    }

    /**
     * Waits briefly for the internal {@link #internalThreadPool()} to terminate. If it does not terminate in time,
     * prints stack traces of the pool threads.
     */
    private void checkThreadTermination() {
        try {
            if (!internalThreadPool().awaitTermination(1, TimeUnit.SECONDS)) {
                getErrorOutput().println("ObjectBox thread pool not terminated in time." +
                        " Ensure all async calls have completed and subscriptions are cancelled before closing the Store." +
                        "\nDumping stack traces of threads on the pool and any using ObjectBox APIs:" +
                        "\n=== BEGIN OF DUMP ===");
                // Note: this may not print any pool threads if other threads are started while enumerating
                // (and the pool threads do not make it into the threads array).
                Thread[] threads = new Thread[Thread.activeCount()];
                int count = Thread.enumerate(threads);
                for (int i = 0; i < count; i++) {
                    Thread thread = threads[i];
                    if (shouldDumpThreadStackTrace(thread)) {
                        getErrorOutput().println("Thread: " + thread.getName());
                        StackTraceElement[] trace = thread.getStackTrace();
                        for (StackTraceElement traceElement : trace) {
                            getErrorOutput().println("\tat " + traceElement);
                        }
                    }
                }
                getErrorOutput().println("=== END OF DUMP ===");
            }
        } catch (InterruptedException e) {
            e.printStackTrace(getErrorOutput());
        }
    }

    private boolean shouldDumpThreadStackTrace(Thread thread) {
        // Dump any threads of the internal thread pool
        if (thread.getName().startsWith(ObjectBoxThreadPool.THREAD_NAME_PREFIX)) return true;

        // Any other thread might be blocking a thread on the internal pool, so also dump any that appear to use
        // ObjectBox APIs.
        StackTraceElement[] trace = thread.getStackTrace();
        for (StackTraceElement traceElement : trace) {
            if (traceElement.getClassName().contains(OBJECTBOX_PACKAGE_NAME)) return true;
        }

        return false;
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
        if (!isClosed()) {
            throw new IllegalStateException("Store must be closed");
        }
        return deleteAllFiles(directory);
    }

    /**
     * Danger zone! This will delete all files in the given directory!
     * <p>
     * No {@link BoxStore} may be alive using the given directory. E.g. call this before building a store. When calling
     * this after {@link #close() closing} a store, read the docs of that method carefully first!
     * <p>
     * If no {@link BoxStoreBuilder#name(String) name} was specified when building the store, use like:
     *
     * <pre>{@code
     *     BoxStore.deleteAllFiles(new File(BoxStoreBuilder.DEFAULT_NAME));
     * }</pre>
     *
     * <p>For an {@link BoxStoreBuilder#inMemory(String) in-memory} database, this will just clean up the in-memory
     * database.
     *
     * @param objectStoreDirectory directory to be deleted; this is the value you previously provided to {@link
     * BoxStoreBuilder#directory(File)}
     * @return true if the directory 1) was deleted successfully OR 2) did not exist in the first place.
     * Note: If false is returned, any number of files may have been deleted before the failure happened.
     * @throws IllegalStateException if the given directory is still used by an open {@link BoxStore}.
     */
    public static boolean deleteAllFiles(File objectStoreDirectory) {
        String canonicalPath = getCanonicalPath(objectStoreDirectory);
        if (isFileOpen(canonicalPath)) {
            throw new IllegalStateException("Cannot delete files: store is still open");
        }
        NativeLibraryLoader.ensureLoaded();
        return nativeRemoveDbFiles(canonicalPath, true);
    }

    /**
     * Danger zone! This will delete all files in the given directory!
     * <p>
     * No {@link BoxStore} may be alive using the given name.
     * <p>
     * If you did not use a custom name with BoxStoreBuilder, you can pass "new File({@link
     * BoxStoreBuilder#DEFAULT_NAME})".
     *
     * @param androidContext provide an Android Context like Application or Service
     * @param customDbNameOrNull use null for default name, or the name you previously provided to {@link
     * BoxStoreBuilder#name(String)}.
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
     * BoxStoreBuilder#baseDirectory(File)}
     * @param customDbNameOrNull use null for default name, or the name you previously provided to {@link
     * BoxStoreBuilder#name(String)}.
     * @return true if the directory 1) was deleted successfully OR 2) did not exist in the first place.
     * Note: If false is returned, any number of files may have been deleted before the failure happened.
     * @throws IllegalStateException if the given directory (+name) is still used by a open {@link BoxStore}.
     */
    public static boolean deleteAllFiles(@Nullable File baseDirectoryOrNull, @Nullable String customDbNameOrNull) {
        File dbDir = BoxStoreBuilder.getDbDir(baseDirectoryOrNull, customDbNameOrNull);
        return deleteAllFiles(dbDir);
    }

    /**
     * Removes all objects from all types ("boxes"), e.g. deletes all database content
     * (excluding meta data like the data model).
     * This typically performs very quickly (e.g. faster than {@link Box#removeAll()}).
     * <p>
     * Note that this does not reclaim disk space: the already reserved space for the DB file(s) is used in the future
     * resulting in better performance because no/less disk allocation has to be done.
     * <p>
     * If you want to reclaim disk space, delete the DB file(s) instead:
     * <ul>
     *     <li>{@link #close()} the BoxStore (and ensure that no thread access it)</li>
     *     <li>{@link #deleteAllFiles()} of the BoxStore</li>
     *     <li>Open a new BoxStore</li>
     * </ul>
     */
    public void removeAllObjects() {
        nativeDropAllData(getNativeStore());
    }

    @Internal
    public void unregisterTransaction(Transaction transaction) {
        synchronized (transactions) {
            transactions.remove(transaction);
            // For close(): notify if there are no more open transactions
            if (!hasActiveTransaction()) {
                transactions.notifyAll();
            }
        }
    }

    /**
     * Returns if {@link #transactions} has a single transaction that {@link Transaction#isActive() isActive()}.
     * <p>
     * Callers must synchronize on {@link #transactions}.
     */
    private boolean hasActiveTransaction() {
        for (Transaction tx : transactions) {
            if (tx.isActive()) {
                return true;
            }
        }
        return false;
    }

    void txCommitted(Transaction tx, @Nullable int[] entityTypeIdsAffected) {
        // Only one write TX at a time, but there is a chance two writers race after commit: thus synchronize
        synchronized (txCommitCountLock) {
            commitCount++; // Overflow is OK because we check for equality
            if (debugTxWrite) {
                getOutput().println("TX committed. New commit count: " + commitCount + ", entity types affected: " +
                        (entityTypeIdsAffected != null ? entityTypeIdsAffected.length : 0));
            }
        }

        if (entityTypeIdsAffected != null) {
            objectClassPublisher.publish(entityTypeIdsAffected);
        }
    }

    /**
     * For all boxes, calls {@link Box#closeActiveTxCursorForCurrentThread(Transaction)}.
     */
    void closeActiveTxCursorsForCurrentThread(Transaction tx) {
        for (Box<?> box : boxes.values()) {
            box.closeActiveTxCursorForCurrentThread(tx);
        }
    }

    /**
     * Returns a Box for the given type. Objects are put into (and get from) their individual Box.
     * <p>
     * Creates a Box only once and then always returns the cached instance.
     */
    @SuppressWarnings("unchecked") // Casting is easier than writing a custom Map.
    public <T> Box<T> boxFor(Class<T> entityClass) {
        Box<T> box = (Box<T>) boxes.get(entityClass);
        if (box == null) {
            if (!dbNameByClass.containsKey(entityClass)) {
                throw new IllegalArgumentException(entityClass +
                        " is not a known entity. Please add it and trigger generation again.");
            }
            // Ensure a box is created just once
            synchronized (boxes) {
                box = (Box<T>) boxes.get(entityClass);
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
                closeActiveTxCursorsForCurrentThread(tx);
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
                closeActiveTxCursorsForCurrentThread(tx);
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
                    getErrorOutput().println(message);
                    e.printStackTrace();
                    getErrorOutput().println(diagnose);
                    getErrorOutput().flush();

                    System.gc();
                    System.runFinalization();
                    cleanStaleReadTransactions();
                }
                if (failedReadTxAttemptCallback != null) {
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
                closeActiveTxCursorsForCurrentThread(tx);
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
                closeActiveTxCursorsForCurrentThread(tx);
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
        internalScheduleThread(() -> {
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
        });
    }

    /**
     * Runs the given Runnable as a transaction in a separate thread.
     * Once the transaction completes the given callback is called (callback may be null).
     * <p>
     * * See also {@link #callInTx(Callable)}.
     */
    public <R> void callInTxAsync(final Callable<R> callable, @Nullable final TxCallback<R> callback) {
        internalScheduleThread(() -> {
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
        });
    }

    /**
     * Gives info that can be useful for debugging.
     *
     * @return String that is typically logged by the application.
     */
    public String diagnose() {
        return nativeDiagnose(getNativeStore());
    }

    /**
     * Validate database pages, a lower level storage unit (integrity check).
     * Do not call this inside a transaction (currently unsupported).
     *
     * @param pageLimit the maximum of pages to validate (e.g. to limit time spent on validation).
     * Pass zero set no limit and thus validate all pages.
     * @param checkLeafLevel Flag to validate leaf pages. These do not point to other pages but contain data.
     * @return Number of pages validated, which may be twice the given pageLimit as internally there are "two DBs".
     * @throws DbException if validation failed to run (does not tell anything about DB file consistency).
     * @throws io.objectbox.exception.FileCorruptException if the DB file is actually inconsistent (corrupt).
     */
    @Beta
    public long validate(long pageLimit, boolean checkLeafLevel) {
        if (pageLimit < 0) {
            throw new IllegalArgumentException("pageLimit must be zero or positive");
        }
        return nativeValidate(getNativeStore(), pageLimit, checkLeafLevel);
    }

    public int cleanStaleReadTransactions() {
        return nativeCleanStaleReadTransactions(getNativeStore());
    }

    /**
     * Frees any cached resources tied to the calling thread (e.g. readers).
     * <p>
     * Call this method from a thread that is about to be shut down or likely not to use ObjectBox anymore.
     * <b>Careful:</b> ensure all transactions, like a query fetching results, have finished before.
     * <p>
     * This method calls {@link Box#closeThreadResources()} for all initiated boxes ({@link #boxFor(Class)}).
     */
    public void closeThreadResources() {
        for (Box<?> box : boxes.values()) {
            box.closeThreadResources();
        }
        // activeTx is cleaned up in finally blocks, so do not free them here
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
        checkOpen();
        return new SubscriptionBuilder<>(objectClassPublisher, null);
    }

    /**
     * Like {@link #subscribe()}, but wires the supplied @{@link io.objectbox.reactive.DataObserver} only to the given
     * object class for notifications.
     */
    @SuppressWarnings("unchecked")
    public <T> SubscriptionBuilder<Class<T>> subscribe(Class<T> forClass) {
        checkOpen();
        return new SubscriptionBuilder<>((DataPublisher) objectClassPublisher, forClass);
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
        String url = nativeStartObjectBrowser(getNativeStore(), null, port);
        if (url != null) {
            objectBrowserPort = port;
        }
        return url;
    }

    @Experimental
    @Nullable
    public String startObjectBrowser(String urlToBindTo) {
        verifyObjectBrowserNotRunning();
        int port;
        try {
            port = new URL(urlToBindTo).getPort(); // Gives -1 if not available
        } catch (MalformedURLException e) {
            throw new RuntimeException("Can not start Object Browser at " + urlToBindTo, e);
        }
        String url = nativeStartObjectBrowser(getNativeStore(), urlToBindTo, 0);
        if (url != null) {
            objectBrowserPort = port;
        }
        return url;
    }

    @Experimental
    public synchronized boolean stopObjectBrowser() {
        if (objectBrowserPort == 0) {
            throw new IllegalStateException("ObjectBrowser has not been started before");
        }
        objectBrowserPort = 0;
        return nativeStopObjectBrowser(getNativeStore());
    }

    @Experimental
    public int getObjectBrowserPort() {
        return objectBrowserPort;
    }

    public boolean isObjectBrowserRunning() {
        return objectBrowserPort != 0;
    }

    private void verifyObjectBrowserNotRunning() {
        if (objectBrowserPort != 0) {
            throw new DbException("ObjectBrowser is already running at port " + objectBrowserPort);
        }
    }

    /**
     * Sets a listener that will be called when an exception is thrown. Replaces a previously set listener.
     * Set to {@code null} to remove the listener.
     * <p>
     * This for example allows central error handling or special logging for database-related exceptions.
     */
    public void setDbExceptionListener(@Nullable DbExceptionListener dbExceptionListener) {
        nativeSetDbExceptionListener(getNativeStore(), dbExceptionListener);
    }

    /**
     * Like {@link Executors#newCachedThreadPool()} but uses {@link ObjectBoxThreadPoolExecutor} to ensure proper
     * cleanup of thread-local resources.
     *
     * @return a new {@link ObjectBoxThreadPoolExecutor}
     */
    public ObjectBoxThreadPoolExecutor newCachedThreadPoolExecutor() {
        return new ObjectBoxThreadPoolExecutor(this, 0, Integer.MAX_VALUE,
                60L, TimeUnit.SECONDS,
                new SynchronousQueue<>());
    }

    /**
     * Like {@link Executors#newCachedThreadPool(ThreadFactory)} but uses {@link ObjectBoxThreadPoolExecutor} to ensure
     * proper cleanup of thread-local resources.
     *
     * @return a new {@link ObjectBoxThreadPoolExecutor}
     */
    public ObjectBoxThreadPoolExecutor newCachedThreadPoolExecutor(ThreadFactory threadFactory) {
        return new ObjectBoxThreadPoolExecutor(this, 0, Integer.MAX_VALUE,
                60L, TimeUnit.SECONDS,
                new SynchronousQueue<>(),
                threadFactory);
    }

    /**
     * Like {@link Executors#newFixedThreadPool(int)} but uses {@link ObjectBoxThreadPoolExecutor} to ensure proper
     * cleanup of thread-local resources.
     *
     * @param nThreads the number of threads in the pool
     * @return a new {@link ObjectBoxThreadPoolExecutor}
     */
    public ObjectBoxThreadPoolExecutor newFixedThreadPoolExecutor(int nThreads) {
        return new ObjectBoxThreadPoolExecutor(this, nThreads, nThreads,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>());
    }

    /**
     * Like {@link Executors#newFixedThreadPool(int, ThreadFactory)} but uses {@link ObjectBoxThreadPoolExecutor} to
     * ensure proper cleanup of thread-local resources.
     *
     * @param nThreads the number of threads in the pool
     * @param threadFactory the factory to use when creating new threads
     * @return a new {@link ObjectBoxThreadPoolExecutor}
     */
    public ObjectBoxThreadPoolExecutor newFixedThreadPoolExecutor(int nThreads, ThreadFactory threadFactory) {
        return new ObjectBoxThreadPoolExecutor(this, nThreads, nThreads,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                threadFactory);
    }

    @Internal
    public Future<?> internalScheduleThread(Runnable runnable) {
        return internalThreadPool().submit(runnable);
    }

    @Internal
    ExecutorService internalThreadPool() {
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
    public TxCallback<?> internalFailedReadTxAttemptCallback() {
        return failedReadTxAttemptCallback;
    }

    /**
     * The output stream to print log messages to. Currently {@link System#out}.
     */
    private PrintStream getOutput() {
        return System.out;
    }

    /**
     * The error output stream to print log messages to. This is {@link System#err} by default.
     */
    private PrintStream getErrorOutput() {
        return errorOutputStream;
    }

    void setDebugFlags(int debugFlags) {
        nativeSetDebugFlags(getNativeStore(), debugFlags);
    }

    long panicModeRemoveAllObjects(int entityId) {
        return nativePanicModeRemoveAllObjects(getNativeStore(), entityId);
    }

    /**
     * Gets the reference to the native store. Can be used with the C API to use the same store, e.g. via JNI, by
     * passing it on to {@code obx_store_wrap()}.
     * <p>
     * Throws if the store is closed.
     * <p>
     * The procedure is like this:<br>
     * 1) you create a BoxStore on the Java side<br>
     * 2) you call this method to get the native store pointer<br>
     * 3) you pass the native store pointer to your native code (e.g. via JNI)<br>
     * 4) your native code calls obx_store_wrap() with the native store pointer to get a OBX_store pointer<br>
     * 5) Using the OBX_store pointer, you can use the C API.
     * <p>
     * Note: Once you {@link #close()} this BoxStore, do not use it from the C API.
     */
    public long getNativeStore() {
        checkOpen();
        return handle;
    }

    /**
     * For internal use only. This API might change or be removed with a future release.
     * <p>
     * Returns {@code true} once the native Store is about to be destroyed.
     * <p>
     * This is {@code true} shortly after {@link #close()} was called and {@link #isClosed()} returns {@code true}.
     *
     * @see #isNativeStoreDestroyed()
     */
    @Internal
    public boolean isNativeStoreClosed() {
        return handle == 0;
    }

    /**
     * For internal use only. This API might change or be removed with a future release.
     * <p>
     * Returns {@code true} once the native Store was destroyed.
     * <p>
     * This is {@code true} shortly after {@link #isNativeStoreClosed()} returns {@code true}.
     *
     * @see #isNativeStoreClosed()
     */
    @Internal
    public boolean isNativeStoreDestroyed() {
        return nativeStoreDestroyed;
    }

    /**
     * Returns the {@link SyncClient} associated with this store. To create one see {@link io.objectbox.sync.Sync Sync}.
     */
    @Nullable
    public SyncClient getSyncClient() {
        return syncClient;
    }

    void setSyncClient(@Nullable SyncClient syncClient) {
        this.syncClient = syncClient;
    }
}
