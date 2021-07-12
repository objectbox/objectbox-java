/*
 * Copyright 2017-2018 ObjectBox Ltd. All rights reserved.
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

import io.objectbox.annotation.IndexType;
import org.junit.After;
import org.junit.Before;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import io.objectbox.ModelBuilder.EntityBuilder;
import io.objectbox.ModelBuilder.PropertyBuilder;
import io.objectbox.model.PropertyFlags;
import io.objectbox.model.PropertyType;


import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public abstract class AbstractObjectBoxTest {

    /**
     * Turns on additional log output, including logging of transactions or query parameters.
     */
    protected static final boolean DEBUG_LOG = false;
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

        // Note: is logged, so create before logging.
        boxStoreDir = prepareTempDir("object-store-test");

        if (!printedVersionsOnce) {
            printedVersionsOnce = true;
            printProcessId();
            System.out.println("ObjectBox Java version: " + BoxStore.getVersion());
            System.out.println("ObjectBox Core version: " + BoxStore.getVersionNative());
            System.out.println("First DB dir: " + boxStoreDir);
        }

        store = createBoxStore();
        runExtensiveTests = System.getProperty("extensive-tests") != null;
    }

    /**
     * This works with Android without needing any context.
     */
    protected File prepareTempDir(String prefix) throws IOException {
        File tempFile = File.createTempFile(prefix, "");
        if (!tempFile.delete()) {
            throw new IOException("Could not prep temp dir; file delete failed for " + tempFile.getAbsolutePath());
        }
        return tempFile;
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
        // Collect dangling Cursors and TXs before store closes
        System.gc();
        System.runFinalization();

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
        deleteAllFiles();
    }

    protected void deleteAllFiles() {
        if (boxStoreDir != null && boxStoreDir.exists()) {
            File[] files = boxStoreDir.listFiles();
            for (File file : files) {
                delete(file);
            }
            delete(boxStoreDir);
        }
    }

    private boolean delete(File file) {
        boolean deleted = file.delete();
        if (!deleted) {
            file.deleteOnExit();
            logError("Could not delete " + file.getAbsolutePath());
        }
        return deleted;
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

        // Unsigned integers.
        entityBuilder.property("simpleShortU", PropertyType.Short).id(TestEntity_.simpleShortU.id, ++lastUid)
                .flags(PropertyFlags.UNSIGNED);
        entityBuilder.property("simpleIntU", PropertyType.Int).id(TestEntity_.simpleIntU.id, ++lastUid)
                .flags(PropertyFlags.UNSIGNED);
        entityBuilder.property("simpleLongU", PropertyType.Long).id(TestEntity_.simpleLongU.id, ++lastUid)
                .flags(PropertyFlags.UNSIGNED);

        int lastId = TestEntity_.simpleLongU.id;
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
        TestEntity entity = new TestEntity();
        entity.setSimpleString(simpleString);
        entity.setSimpleInt(nr);
        entity.setSimpleByte((byte) (10 + nr));
        entity.setSimpleBoolean(nr % 2 == 0);
        entity.setSimpleShort((short) (100 + nr));
        entity.setSimpleLong(1000 + nr);
        entity.setSimpleFloat(200 + nr / 10f);
        entity.setSimpleDouble(2000 + nr / 100f);
        entity.setSimpleByteArray(new byte[]{1, 2, (byte) nr});
        entity.setSimpleStringArray(new String[]{simpleString});
        entity.setSimpleShortU((short) (100 + nr));
        entity.setSimpleIntU(nr);
        entity.setSimpleLongU(1000 + nr);
        return entity;
    }

    /**
     * Asserts all properties, excluding id. Assumes entity was created with {@link #createTestEntity(String, int)}.
     */
    protected void assertTestEntity(TestEntity actual, @Nullable String simpleString, int nr) {
        assertEquals(simpleString, actual.getSimpleString());
        assertEquals(nr, actual.getSimpleInt());
        assertEquals((byte) (10 + nr), actual.getSimpleByte());
        assertEquals(nr % 2 == 0, actual.getSimpleBoolean());
        assertEquals((short) (100 + nr), actual.getSimpleShort());
        assertEquals(1000 + nr, actual.getSimpleLong());
        assertEquals(200 + nr / 10f, actual.getSimpleFloat(), 0);
        assertEquals(2000 + nr / 100f, actual.getSimpleDouble(), 0);
        assertArrayEquals(new byte[]{1, 2, (byte) nr}, actual.getSimpleByteArray());
        // null array items are ignored, so array will be empty
        String[] expectedStringArray = simpleString == null ? new String[]{} : new String[]{simpleString};
        assertArrayEquals(expectedStringArray, actual.getSimpleStringArray());
        assertEquals((short) (100 + nr), actual.getSimpleShortU());
        assertEquals(nr, actual.getSimpleIntU());
        assertEquals(1000 + nr, actual.getSimpleLongU());
    }

    protected TestEntity putTestEntity(@Nullable String simpleString, int nr) {
        TestEntity entity = createTestEntity(simpleString, nr);
        long key = getTestEntityBox().put(entity);
        assertTrue(key != 0);
        assertEquals(key, entity.getId());
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