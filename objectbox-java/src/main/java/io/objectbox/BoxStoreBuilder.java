/*
 * Copyright 2017 ObjectBox Ltd. All rights reserved.
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

import org.greenrobot.essentials.io.IoUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import io.objectbox.annotation.apihint.Experimental;
import io.objectbox.annotation.apihint.Internal;
import io.objectbox.exception.DbException;
import io.objectbox.ideasonly.ModelUpdate;

/**
 * Builds a {@link BoxStore} with optional configurations. The class is not initiated directly; use
 * MyObjectBox.builder() to get an instance.
 * <p>
 * Each configuration has reasonable defaults, which you can adjust.
 * For Android, you must pass a Context using {@link #androidContext(Object)}.
 * <p>
 * Configurations you can override:
 * <ol>
 * <li>Name/location of DB: use {@link #name(String)}/{@link #baseDirectory}/{@link #androidContext(Object)}
 * OR {@link #directory(File)}(default: name "objectbox)</li>
 * <li>Max DB size: see {@link #maxSizeInKByte} (default: 1 GB)</li>
 * <li>Max readers: see {@link #maxReaders(int)} (default: 126)</li>
 * </ol>
 */
public class BoxStoreBuilder {

    /** The default DB name, which can be overwritten using {@link #name(String)}. */
    public static final String DEFAULT_NAME = "objectbox";

    /** The default maximum size the DB can grow to, which can be overwritten using {@link #maxSizeInKByte}. */
    public static final int DEFAULT_MAX_DB_SIZE_KBYTE = 1024 * 1024;

    final byte[] model;

    /** BoxStore uses this */
    File directory;

    /** On Android used for native library loading. */
    @Nullable Object context;
    @Nullable Object relinker;

    /** Ignored by BoxStore */
    private File baseDirectory;

    /** Ignored by BoxStore */
    private String name;

    /** Defaults to {@link #DEFAULT_MAX_DB_SIZE_KBYTE}. */
    long maxSizeInKByte = DEFAULT_MAX_DB_SIZE_KBYTE;

    ModelUpdate modelUpdate;

    int debugFlags;

    private boolean android;

    boolean debugRelations;

    int maxReaders;

    int queryAttempts;

    TxCallback<?> failedReadTxAttemptCallback;

    final List<EntityInfo<?>> entityInfoList = new ArrayList<>();
    private Factory<InputStream> initialDbFileFactory;

    /** Not for application use. */
    public static BoxStoreBuilder createDebugWithoutModel() {
        return new BoxStoreBuilder();
    }

    private BoxStoreBuilder() {
        model = null;
    }

    /** Called internally from the generated class "MyObjectBox". Check MyObjectBox.builder() to get an instance. */
    @Internal
    public BoxStoreBuilder(byte[] model) {
        this.model = model;
        if (model == null) {
            throw new IllegalArgumentException("Model may not be null");
        }
    }

    /**
     * Name of the database, which will be used as a directory for DB files.
     * You can also specify a base directory for this one using {@link #baseDirectory(File)}.
     * Cannot be used in combination with {@link #directory(File)}.
     * <p>
     * Default: "objectbox", {@link #DEFAULT_NAME} (unless {@link #directory(File)} is used)
     */
    public BoxStoreBuilder name(String name) {
        if (directory != null) {
            throw new IllegalArgumentException("Already has directory, cannot assign name");
        }
        if (name.contains("/") || name.contains("\\")) {
            throw new IllegalArgumentException("Name may not contain (back) slashes. " +
                    "Use baseDirectory() or directory() to configure alternative directories");
        }
        this.name = name;
        return this;
    }

    /**
     * The directory where all DB files should be placed in.
     * Cannot be used in combination with {@link #name(String)}/{@link #baseDirectory(File)}.
     */
    public BoxStoreBuilder directory(File directory) {
        if (name != null) {
            throw new IllegalArgumentException("Already has name, cannot assign directory");
        }
        if (!android && baseDirectory != null) {
            throw new IllegalArgumentException("Already has base directory, cannot assign directory");
        }
        this.directory = directory;
        return this;
    }

    /**
     * In combination with {@link #name(String)}, this lets you specify the location of where the DB files should be
     * stored.
     * Cannot be used in combination with {@link #directory(File)}.
     */
    public BoxStoreBuilder baseDirectory(File baseDirectory) {
        if (directory != null) {
            throw new IllegalArgumentException("Already has directory, cannot assign base directory");
        }
        this.baseDirectory = baseDirectory;
        return this;
    }

    /**
     * On Android, you can pass a Context to set the base directory using this method.
     * This will conveniently configure the storage location to be in the files directory of your app.
     * <p>
     * In more detail, this assigns the base directory (see {@link #baseDirectory}) to
     * {@code context.getFilesDir() + "/objectbox/"}.
     * Thus, when using the default name (also "objectbox" unless overwritten using {@link #name(String)}), the default
     * location of DB files will be "objectbox/objectbox/" inside the app files directory.
     * If you specify a custom name, for example with {@code name("foobar")}, it would become
     * "objectbox/foobar/".
     * <p>
     * Alternatively, you can also use {@link #baseDirectory} or {@link #directory(File)} instead.
     */
    public BoxStoreBuilder androidContext(Object context) {
        //noinspection ConstantConditions Annotation does not enforce non-null.
        if (context == null) {
            throw new NullPointerException("Context may not be null");
        }
        this.context = getApplicationContext(context);

        File baseDir = getAndroidBaseDir(context);
        if (!baseDir.exists()) {
            baseDir.mkdir();
            if (!baseDir.exists()) { // check baseDir.exists() because of potential concurrent processes
                throw new RuntimeException("Could not init Android base dir at " + baseDir.getAbsolutePath());
            }
        }
        if (!baseDir.isDirectory()) {
            throw new RuntimeException("Android base dir is not a dir: " + baseDir.getAbsolutePath());
        }
        baseDirectory = baseDir;
        android = true;
        return this;
    }

    private Object getApplicationContext(Object context) {
        try {
            return context.getClass().getMethod("getApplicationContext").invoke(context);
        } catch (Exception e) {
            // note: can't catch ReflectiveOperationException, is K+ (19+) on Android
            throw new RuntimeException("context must be a valid Android Context", e);
        }
    }

    /**
     * Pass a custom ReLinkerInstance, for example {@code ReLinker.log(logger)} to use for loading the native library
     * on Android devices. Note that setting {@link #androidContext(Object)} is required for ReLinker to work.
     */
    public BoxStoreBuilder androidReLinker(Object reLinkerInstance) {
        if (context == null) {
            throw new IllegalArgumentException("Set a Context using androidContext(context) first");
        }
        //noinspection ConstantConditions Annotation does not enforce non-null.
        if (reLinkerInstance == null) {
            throw new NullPointerException("ReLinkerInstance may not be null");
        }
        this.relinker = reLinkerInstance;
        return this;
    }

    static File getAndroidDbDir(Object context, @Nullable String dbName) {
        File baseDir = getAndroidBaseDir(context);
        return new File(baseDir, dbName(dbName));
    }

    private static String dbName(@Nullable String dbNameOrNull) {
        return dbNameOrNull != null ? dbNameOrNull : DEFAULT_NAME;
    }

    static File getAndroidBaseDir(Object context) {
        return new File(getAndroidFilesDir(context), "objectbox");
    }

    @Nonnull
    private static File getAndroidFilesDir(Object context) {
        File filesDir;
        try {
            Method getFilesDir = context.getClass().getMethod("getFilesDir");
            filesDir = (File) getFilesDir.invoke(context);
            if (filesDir == null) {
                // Race condition in Android before 4.4: https://issuetracker.google.com/issues/36918154 ?
                System.err.println("getFilesDir() returned null - retrying once...");
                filesDir = (File) getFilesDir.invoke(context);
            }
        } catch (Exception e) {
            throw new RuntimeException(
                    "Could not init with given Android context (must be sub class of android.content.Context)", e);
        }
        if (filesDir == null) {
            throw new IllegalStateException("Android files dir is null");
        }
        if (!filesDir.exists()) {
            throw new IllegalStateException("Android files dir does not exist");
        }
        return filesDir;
    }

    /**
     * Sets the maximum number of concurrent readers. For most applications, the default is fine (&gt; 100 readers).
     * <p>
     * A "reader" is short for a thread involved in a read transaction.
     * <p>
     * If you hit {@link io.objectbox.exception.DbMaxReadersExceededException}, you should first worry about the
     * amount of threads you are using.
     * For highly concurrent setups (e.g. you are using ObjectBox on the server side) it may make sense to increase the
     * number.
     */

    public BoxStoreBuilder maxReaders(int maxReaders) {
        this.maxReaders = maxReaders;
        return this;
    }

    @Internal
    public void entity(EntityInfo<?> entityInfo) {
        entityInfoList.add(entityInfo);
    }

    // Not sure this will ever be implements
    BoxStoreBuilder modelUpdate(ModelUpdate modelUpdate) {
        throw new UnsupportedOperationException("Not yet implemented");
        //        this.modelUpdate = modelUpdate;
        //        return this;
    }

    /**
     * Sets the maximum size the database file can grow to.
     * By default this is 1 GB, which should be sufficient for most applications.
     * <p>
     * In general, a maximum size prevents the DB from growing indefinitely when something goes wrong
     * (for example you insert data in an infinite loop).
     */
    public BoxStoreBuilder maxSizeInKByte(long maxSizeInKByte) {
        this.maxSizeInKByte = maxSizeInKByte;
        return this;
    }

    /**
     * @deprecated Use {@link #debugFlags} instead.
     */
    @Deprecated
    public BoxStoreBuilder debugTransactions() {
        this.debugFlags |= DebugFlags.LOG_TRANSACTIONS_READ | DebugFlags.LOG_TRANSACTIONS_WRITE;
        return this;
    }

    /**
     * Debug flags typically enable additional logging, see {@link DebugFlags} for valid values.
     * <p>
     * Example: debugFlags({@link DebugFlags#LOG_TRANSACTIONS_READ} | {@link DebugFlags#LOG_TRANSACTIONS_WRITE});
     */
    public BoxStoreBuilder debugFlags(int debugFlags) {
        this.debugFlags = debugFlags;
        return this;
    }

    /** Enables some debug logging for relations. */
    public BoxStoreBuilder debugRelations() {
        this.debugRelations = true;
        return this;
    }

    /**
     * For massive concurrent setups (app is using a lot of threads), you can enable automatic retries for queries.
     * This can resolve situations in which resources are getting sparse (e.g.
     * {@link io.objectbox.exception.DbMaxReadersExceededException} or other variations of
     * {@link io.objectbox.exception.DbException} are thrown during query execution).
     *
     * @param queryAttempts number of attempts a query find operation will be executed before failing.
     *                      Recommended values are in the range of 2 to 5, e.g. a value of 3 as a starting point.
     */
    @Experimental
    public BoxStoreBuilder queryAttempts(int queryAttempts) {
        if (queryAttempts < 1) {
            throw new IllegalArgumentException("Query attempts must >= 1");
        }
        this.queryAttempts = queryAttempts;
        return this;
    }

    /**
     * Define a callback for failed read transactions during retires (see also {@link #queryAttempts(int)}).
     * Useful for e.g. logging.
     */
    @Experimental
    public BoxStoreBuilder failedReadTxAttemptCallback(TxCallback<?> failedReadTxAttemptCallback) {
        this.failedReadTxAttemptCallback = failedReadTxAttemptCallback;
        return this;
    }

    /**
     * Let's you specify an DB file to be used during initial start of the app (no DB file exists yet).
     */
    @Experimental
    public BoxStoreBuilder initialDbFile(final File initialDbFile) {
        return initialDbFile(() -> new FileInputStream(initialDbFile));
    }

    /**
     * Let's you specify a provider for a DB file to be used during initial start of the app (no DB file exists yet).
     * The provider will only be called if no DB file exists yet.
     */
    @Experimental
    public BoxStoreBuilder initialDbFile(Factory<InputStream> initialDbFileFactory) {
        this.initialDbFileFactory = initialDbFileFactory;
        return this;
    }

    /**
     * Builds a {@link BoxStore} using any given configuration.
     */
    public BoxStore build() {
        if (directory == null) {
            name = dbName(name);
            directory = getDbDir(baseDirectory, name);
        }
        checkProvisionInitialDbFile();
        return new BoxStore(this);
    }

    private void checkProvisionInitialDbFile() {
        if (initialDbFileFactory != null) {
            String dataDir = BoxStore.getCanonicalPath(directory);
            File file = new File(dataDir, "data.mdb");
            if (!file.exists()) {
                InputStream in = null;
                OutputStream out = null;
                try {
                    in = initialDbFileFactory.provide();
                    if (in == null) {
                        throw new DbException("Factory did not provide a resource");
                    }
                    in = new BufferedInputStream(in);
                    out = new BufferedOutputStream(new FileOutputStream(file));
                    IoUtils.copyAllBytes(in, out);
                } catch (Exception e) {
                    throw new DbException("Could not provision initial data file", e);
                } finally {
                    IoUtils.safeClose(out);
                    IoUtils.safeClose(in);
                }
            }
        }
    }

    static File getDbDir(@Nullable File baseDirectoryOrNull, @Nullable String nameOrNull) {
        String name = dbName(nameOrNull);
        if (baseDirectoryOrNull != null) {
            return new File(baseDirectoryOrNull, name);
        } else {
            return new File(name);
        }
    }

    /**
     * Builds the default {@link BoxStore} instance, which can be acquired using {@link BoxStore#getDefault()}.
     * For testability, please see the comment of {@link BoxStore#getDefault()}.
     * <p>
     * May be called once only (throws otherwise).
     */
    public BoxStore buildDefault() {
        BoxStore store = build();
        BoxStore.setDefault(store);
        return store;
    }
}
