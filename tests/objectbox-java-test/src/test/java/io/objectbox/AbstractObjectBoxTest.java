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

import org.junit.After;
import org.junit.Before;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import io.objectbox.ModelBuilder.EntityBuilder;
import io.objectbox.ModelBuilder.PropertyBuilder;
import io.objectbox.annotation.IndexType;
import io.objectbox.config.DebugFlags;
import io.objectbox.model.PropertyFlags;
import io.objectbox.model.PropertyType;
import io.objectbox.query.InternalAccess;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public abstract class AbstractObjectBoxTest {

    /**
     * Turns on additional log output, including logging of transactions or query parameters.
     */
    protected static final boolean DEBUG_LOG = false;

    /**
     * If instead of files the database should be in memory.
     */
    protected static final boolean IN_MEMORY = Objects.equals(System.getProperty("obx.inMemory"), "true");
    private static boolean printedVersionsOnce;

    protected File boxStoreDir;
    protected BoxStore store;
    protected Random random = new Random();
    protected boolean runExtensiveTests;

    int lastEntityId;
    int lastIndexId;
    long lastUid;
    long lastEntityUid;
    long lastIndexUid;

    static void printProcessId() {
        try {
            // Only if Java 9 is available; e.g. helps to attach native debugger
            Class<?> processHandleClass = Class.forName("java.lang.ProcessHandle");
            Object processHandle = processHandleClass.getMethod("current").invoke(null);
            long pid = (Long) processHandleClass.getMethod("pid").invoke(processHandle);
            System.out.println("ObjectBox test process ID (pid): " + pid);
            System.out.flush();
        } catch (Throwable th) {
            System.out.println("Could not get process ID (" + th.getMessage() + ")");
        }
    }

    @Before
    public void setUp() throws IOException {
        Cursor.TRACK_CREATION_STACK = true;
        Transaction.TRACK_CREATION_STACK = true;
        InternalAccess.queryPublisherLogStates();

        // Note: is logged, so create before logging.
        boxStoreDir = prepareTempDir("object-store-test");

        if (!printedVersionsOnce) {
            printedVersionsOnce = true;
            printProcessId();
            System.out.println("ObjectBox Java SDK version: " + BoxStore.getVersion());
            System.out.println("ObjectBox Database version: " + BoxStore.getVersionNative());
            System.out.println("First DB dir: " + boxStoreDir);
            System.out.println("IN_MEMORY=" + IN_MEMORY);
            System.out.println("java.version=" + System.getProperty("java.version"));
            System.out.println("java.vendor=" + System.getProperty("java.vendor"));
            System.out.println("file.encoding=" + System.getProperty("file.encoding"));
            System.out.println("sun.jnu.encoding=" + System.getProperty("sun.jnu.encoding"));
        }

        store = createBoxStore();
        runExtensiveTests = System.getProperty("extensive-tests") != null;
    }

    /**
     * This works with Android without needing any context.
     */
    protected File prepareTempDir(String prefix) throws IOException {
        if (IN_MEMORY) {
            // Instead of random temp directory, use random suffix for each test to avoid re-using existing database
            // from other tests in case clean-up fails.
            // Note: tearDown code will still work as the directory does not exist.
            String randomPart = Long.toUnsignedString(random.nextLong());
            return new File(BoxStore.IN_MEMORY_PREFIX + prefix + randomPart);
        } else {
            File tempFile = File.createTempFile(prefix, "");
            if (!tempFile.delete()) {
                throw new IOException("Could not prep temp dir; file delete failed for " + tempFile.getAbsolutePath());
            }
            return tempFile;
        }
    }

    protected BoxStore createBoxStore() {
        return createBoxStore(null);
    }

    protected BoxStore createBoxStore(@Nullable IndexType simpleStringIndexType) {
        return createBoxStoreBuilder(simpleStringIndexType).build();
    }

    protected BoxStoreBuilder createBoxStoreBuilder(@Nullable IndexType simpleStringIndexType) {
        BoxStoreBuilder builder = new BoxStoreBuilder(createTestModel(simpleStringIndexType)).directory(boxStoreDir);
        if (DEBUG_LOG) builder.debugFlags(DebugFlags.LOG_TRANSACTIONS_READ | DebugFlags.LOG_TRANSACTIONS_WRITE);
        builder.entity(new TestEntity_());
        return builder;
    }

    protected BoxStoreBuilder createBoxStoreBuilderWithTwoEntities(boolean withIndex) {
        BoxStoreBuilder builder = new BoxStoreBuilder(createTestModelWithTwoEntities(withIndex)).directory(boxStoreDir);
        if (DEBUG_LOG) builder.debugFlags(DebugFlags.LOG_TRANSACTIONS_READ | DebugFlags.LOG_TRANSACTIONS_WRITE);
        builder.entity(new TestEntity_());
        builder.entity(new TestEntityMinimal_());
        return builder;
    }

    protected Box<TestEntity> getTestEntityBox() {
        return store.boxFor(TestEntity.class);
    }

    @After
    public void tearDown() {
        // Note: do not collect dangling Cursors and Transactions before store closes (using System.gc()
        // or System.runFinalization()). Tests should mirror user code and do that themselves (calling close())
        // or rely on the library (through finalizers or BoxStore.close()).

        if (store != null) {
            try {
                store.close();
                store.deleteAllFiles();

                File[] files = boxStoreDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        logError("File was not deleted: " + file.getAbsolutePath());
                    }
                }
            } catch (Exception e) {
                logError("Could not clean up test", e);
            }
        }
        cleanUpAllFiles(boxStoreDir);
    }

    /**
     * Manually clean up any leftover files to prevent interference with other tests.
     */
    protected void cleanUpAllFiles(@Nullable File boxStoreDir) {
        if (boxStoreDir != null && boxStoreDir.exists()) {
            try (Stream<Path> stream = Files.walk(boxStoreDir.toPath())) {
                stream.sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                logError("Could not delete file", e);
                                fail("Could not delete file");
                            }
                        });
            } catch (IOException e) {
                logError("Could not delete file", e);
                fail("Could not delete file");
            }
        }
    }

    protected void log(String text) {
        System.out.println(text);
    }

    protected void logError(String text) {
        System.err.println(text);
    }

    protected void logError(@Nullable String text, Exception ex) {
        if (text != null) {
            System.err.println(text);
        }
        ex.printStackTrace();
    }

    protected long time() {
        return System.currentTimeMillis();
    }

    protected byte[] createTestModel(@Nullable IndexType simpleStringIndexType) {
        ModelBuilder modelBuilder = new ModelBuilder();
        addTestEntity(modelBuilder, simpleStringIndexType);
        modelBuilder.lastEntityId(lastEntityId, lastEntityUid);
        modelBuilder.lastIndexId(lastIndexId, lastIndexUid);
        return modelBuilder.build();
    }

    byte[] createTestModelWithTwoEntities(boolean withIndex) {
        ModelBuilder modelBuilder = new ModelBuilder();
        addTestEntity(modelBuilder, withIndex ? IndexType.DEFAULT : null);
        addTestEntityMinimal(modelBuilder, withIndex);
        modelBuilder.lastEntityId(lastEntityId, lastEntityUid);
        modelBuilder.lastIndexId(lastIndexId, lastIndexUid);
        return modelBuilder.build();
    }

    /**
     * When not using the {@link #store} of this to create a builder with the default test model.
     */
    protected BoxStoreBuilder createBuilderWithTestModel() {
        return new BoxStoreBuilder(createTestModel(null));
    }

    private void addTestEntity(ModelBuilder modelBuilder, @Nullable IndexType simpleStringIndexType) {
        lastEntityUid = ++lastUid;
        EntityBuilder entityBuilder = modelBuilder.entity("TestEntity").id(++lastEntityId, lastEntityUid);
        entityBuilder.property("id", PropertyType.Long).id(TestEntity_.id.id, ++lastUid)
                .flags(PropertyFlags.ID | (areIdsAssignable() ? PropertyFlags.ID_SELF_ASSIGNABLE : 0));
        entityBuilder.property("simpleBoolean", PropertyType.Bool).id(TestEntity_.simpleBoolean.id, ++lastUid);
        entityBuilder.property("simpleByte", PropertyType.Byte).id(TestEntity_.simpleByte.id, ++lastUid);
        entityBuilder.property("simpleShort", PropertyType.Short).id(TestEntity_.simpleShort.id, ++lastUid);
        entityBuilder.property("simpleInt", PropertyType.Int).id(TestEntity_.simpleInt.id, ++lastUid);
        entityBuilder.property("simpleLong", PropertyType.Long).id(TestEntity_.simpleLong.id, ++lastUid);
        entityBuilder.property("simpleFloat", PropertyType.Float).id(TestEntity_.simpleFloat.id, ++lastUid);
        entityBuilder.property("simpleDouble", PropertyType.Double).id(TestEntity_.simpleDouble.id, ++lastUid);
        PropertyBuilder pb =
                entityBuilder.property("simpleString", PropertyType.String).id(TestEntity_.simpleString.id, ++lastUid);
        if (simpleStringIndexType != null) {
            lastIndexUid = ++lastUid;
            // Since 2.0: default for Strings has changed from INDEXED to INDEX_HASH.
            int indexFlag;
            if (simpleStringIndexType == IndexType.VALUE) {
                indexFlag = PropertyFlags.INDEXED;
            } else if (simpleStringIndexType == IndexType.HASH64) {
                indexFlag = PropertyFlags.INDEX_HASH64;
            } else {
                indexFlag = PropertyFlags.INDEX_HASH;
            }
            log(String.format("Using %s index on TestEntity.simpleString", simpleStringIndexType));
            pb.flags(indexFlag).indexId(++lastIndexId, lastIndexUid);
        }
        entityBuilder.property("simpleByteArray", PropertyType.ByteVector).id(TestEntity_.simpleByteArray.id, ++lastUid);
        entityBuilder.property("simpleStringArray", PropertyType.StringVector).id(TestEntity_.simpleStringArray.id, ++lastUid);
        entityBuilder.property("simpleStringList", PropertyType.StringVector).id(TestEntity_.simpleStringList.id, ++lastUid)
                .flags(PropertyFlags.NON_PRIMITIVE_TYPE);

        // Unsigned integers.
        entityBuilder.property("simpleShortU", PropertyType.Short).id(TestEntity_.simpleShortU.id, ++lastUid)
                .flags(PropertyFlags.UNSIGNED);
        entityBuilder.property("simpleIntU", PropertyType.Int).id(TestEntity_.simpleIntU.id, ++lastUid)
                .flags(PropertyFlags.UNSIGNED);
        entityBuilder.property("simpleLongU", PropertyType.Long).id(TestEntity_.simpleLongU.id, ++lastUid)
                .flags(PropertyFlags.UNSIGNED);

        // Flexible properties
        entityBuilder.property("stringObjectMap", PropertyType.Flex)
                .id(TestEntity_.stringObjectMap.id, ++lastUid);
        entityBuilder.property("flexProperty", PropertyType.Flex).id(TestEntity_.flexProperty.id, ++lastUid);

        // Integer and floating point arrays
        entityBuilder.property("booleanArray", PropertyType.BoolVector).id(TestEntity_.booleanArray.id, ++lastUid);
        entityBuilder.property("shortArray", PropertyType.ShortVector).id(TestEntity_.shortArray.id, ++lastUid);
        entityBuilder.property("charArray", PropertyType.CharVector).id(TestEntity_.charArray.id, ++lastUid);
        entityBuilder.property("intArray", PropertyType.IntVector).id(TestEntity_.intArray.id, ++lastUid);
        entityBuilder.property("longArray", PropertyType.LongVector).id(TestEntity_.longArray.id, ++lastUid);
        entityBuilder.property("floatArray", PropertyType.FloatVector).id(TestEntity_.floatArray.id, ++lastUid);
        entityBuilder.property("doubleArray", PropertyType.DoubleVector).id(TestEntity_.doubleArray.id, ++lastUid);

        // Date property
        entityBuilder.property("date", PropertyType.Date).id(TestEntity_.date.id, ++lastUid);
        int lastId = TestEntity_.date.id;

        entityBuilder.lastPropertyId(lastId, lastUid);
        addOptionalFlagsToTestEntity(entityBuilder);
        entityBuilder.entityDone();
    }

    protected void addOptionalFlagsToTestEntity(EntityBuilder entityBuilder) {
    }

    private void addTestEntityMinimal(ModelBuilder modelBuilder, boolean withIndex) {
        lastEntityUid = ++lastUid;
        EntityBuilder entityBuilder = modelBuilder.entity("TestEntityMinimal").id(++lastEntityId, lastEntityUid);
        int pId = 0;
        entityBuilder.property("id", PropertyType.Long).id(++pId, ++lastUid).flags(PropertyFlags.ID);
        long lastPropertyUid = ++lastUid;
        PropertyBuilder pb = entityBuilder.property("text", PropertyType.String).id(++pId, lastPropertyUid);
        if (withIndex) {
            lastIndexUid = ++lastUid;
            pb.flags(PropertyFlags.INDEXED).indexId(++lastIndexId, lastIndexUid);
        }
        entityBuilder.lastPropertyId(pId, lastPropertyUid);
        entityBuilder.entityDone();
    }

    protected TestEntity createTestEntity(@Nullable String simpleString, int nr) {
        boolean simpleBoolean = nr % 2 == 0;
        short simpleShort = (short) (100 + nr);
        int simpleLong = 1000 + nr;
        float simpleFloat = 200 + nr / 10f;
        double simpleDouble = 2000 + nr / 100f;
        byte[] simpleByteArray = {1, 2, (byte) nr};
        String[] simpleStringArray = {simpleString};

        TestEntity entity = new TestEntity();
        entity.setSimpleString(simpleString);
        entity.setSimpleInt(nr);
        entity.setSimpleByte((byte) (10 + nr));
        entity.setSimpleBoolean(simpleBoolean);
        entity.setSimpleShort(simpleShort);
        entity.setSimpleLong(simpleLong);
        entity.setSimpleFloat(simpleFloat);
        entity.setSimpleDouble(simpleDouble);
        entity.setSimpleByteArray(simpleByteArray);
        entity.setSimpleStringArray(simpleStringArray);
        entity.setSimpleStringList(Arrays.asList(simpleStringArray));
        entity.setSimpleShortU(simpleShort);
        entity.setSimpleIntU(nr);
        entity.setSimpleLongU(simpleLong);
        if (simpleString != null) {
            Map<String, Object> stringObjectMap = new HashMap<>();
            stringObjectMap.put(simpleString, simpleString);
            entity.setStringObjectMap(stringObjectMap);
        }
        entity.setFlexProperty(simpleString);
        entity.setBooleanArray(new boolean[]{simpleBoolean, false, true});
        entity.setShortArray(new short[]{(short) -(100 + nr), simpleShort});
        entity.setCharArray(simpleString != null ? simpleString.toCharArray() : null);
        entity.setIntArray(new int[]{-nr, nr});
        entity.setLongArray(new long[]{-simpleLong, simpleLong});
        entity.setFloatArray(new float[]{-simpleFloat, simpleFloat});
        entity.setDoubleArray(new double[]{-simpleDouble, simpleDouble});
        entity.setDate(new Date(simpleLong));
        return entity;
    }

    protected TestEntity putTestEntity(@Nullable String simpleString, int nr) {
        TestEntity entity = createTestEntity(simpleString, nr);
        long id = getTestEntityBox().put(entity);
        assertTrue(id != 0);
        assertEquals(id, entity.getId());
        return entity;
    }

    protected List<TestEntity> putTestEntities(int count, @Nullable String baseString, int baseNr) {
        List<TestEntity> entities = new ArrayList<>();
        for (int i = baseNr; i < baseNr + count; i++) {
            entities.add(createTestEntity(baseString != null ? baseString + i : null, i));
        }
        getTestEntityBox().put(entities);
        return entities;
    }

    protected List<TestEntity> putTestEntities(int count) {
        return putTestEntities(count, "foo", 1);
    }

    protected void assertLatchCountedDown(CountDownLatch latch, int seconds) {
        try {
            assertTrue(latch.await(seconds, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    protected boolean areIdsAssignable() {
        return false;
    }

}