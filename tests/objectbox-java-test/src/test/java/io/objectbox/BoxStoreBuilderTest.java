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

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.objectbox.exception.DbFullException;
import io.objectbox.exception.DbMaxDataSizeExceededException;


import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeFalse;

public class BoxStoreBuilderTest extends AbstractObjectBoxTest {

    private BoxStoreBuilder builder;

    private static final String LONG_STRING = "Lorem ipsum dolor sit amet, consectetur adipiscing elit.";

    @Override
    protected BoxStore createBoxStore() {
        // Standard setup of store not required
        return null;
    }

    @Before
    public void setUpBuilder() {
        BoxStore.clearDefaultStore();
        builder = createBuilderWithTestModel().directory(boxStoreDir);
    }

    @Test
    public void testDefaultStore() {
        BoxStore boxStore = builder.buildDefault();
        assertSame(boxStore, BoxStore.getDefault());
        assertSame(boxStore, BoxStore.getDefault());
        boxStore.close(); // to prevent "Another BoxStore was opened" error
        try {
            builder.buildDefault();
            fail("Should have thrown");
        } catch (IllegalStateException expected) {
            // OK
        }
    }

    @Test
    public void testClearDefaultStore() {
        BoxStore boxStore1 = builder.buildDefault();
        BoxStore.clearDefaultStore();
        try {
            BoxStore.getDefault();
            fail("Should have thrown");
        } catch (IllegalStateException expected) {
            // OK
        }
        boxStore1.close();
        BoxStore boxStore = builder.buildDefault();
        assertSame(boxStore, BoxStore.getDefault());
        boxStore.close();
    }

    @Test(expected = IllegalStateException.class)
    public void testDefaultStoreNull() {
        BoxStore.getDefault();
    }

    @Test
    public void directoryUnicodePath() throws IOException {
        File parentTestDir = new File("unicode-test");
        File testDir = new File(parentTestDir, "Îñţérñåţîöñåļîžåţîờñ");
        builder.directory(testDir);
        BoxStore store = builder.build();
        store.close();

        // Check only expected files and directories exist.
        // Note: can not compare Path objects, does not appear to work on macOS for unknown reason.
        Set<String> expectedPaths = new HashSet<>();
        expectedPaths.add(parentTestDir.toPath().toString());
        expectedPaths.add(testDir.toPath().toString());
        Path testDirPath = testDir.toPath();
        expectedPaths.add(testDirPath.resolve("data.mdb").toString());
        expectedPaths.add(testDirPath.resolve("lock.mdb").toString());
        try (Stream<Path> files = Files.walk(parentTestDir.toPath())) {
            List<Path> unexpectedPaths = files.filter(path -> !expectedPaths.remove(path.toString())).collect(Collectors.toList());
            if (!unexpectedPaths.isEmpty()) {
                fail("Found unexpected paths: " + unexpectedPaths);
            }
            if (!expectedPaths.isEmpty()) {
                fail("Missing expected paths: " + expectedPaths);
            }
        }

        cleanUpAllFiles(parentTestDir);
    }

    @Test
    public void directoryConflictingOptionsError() {
        // using conflicting option after directory option
        assertThrows(IllegalStateException.class, () -> createBuilderWithTestModel()
                .directory(boxStoreDir)
                .name("options-test")
        );
        assertThrows(IllegalStateException.class, () -> createBuilderWithTestModel()
                .directory(boxStoreDir)
                .baseDirectory(boxStoreDir)
        );

        // using directory option after conflicting option
        assertThrows(IllegalStateException.class, () -> createBuilderWithTestModel()
                .name("options-test")
                .directory(boxStoreDir)
        );
        assertThrows(IllegalStateException.class, () -> createBuilderWithTestModel()
                .baseDirectory(boxStoreDir)
                .directory(boxStoreDir)
        );
    }

    @Test
    public void inMemoryConflictingOptionsError() {
        // directory-based option after switching to in-memory
        assertThrows(IllegalStateException.class, () -> createBuilderWithTestModel()
                .inMemory("options-test")
                .name("options-test")
        );
        assertThrows(IllegalStateException.class, () -> createBuilderWithTestModel()
                .inMemory("options-test")
                .directory(boxStoreDir)
        );
        assertThrows(IllegalStateException.class, () -> createBuilderWithTestModel()
                .inMemory("options-test")
                .baseDirectory(boxStoreDir)
        );

        // in-memory after specifying directory-based option
        assertThrows(IllegalStateException.class, () -> createBuilderWithTestModel()
                .name("options-test")
                .inMemory("options-test")
        );
        assertThrows(IllegalStateException.class, () -> createBuilderWithTestModel()
                .directory(boxStoreDir)
                .inMemory("options-test")
        );
        assertThrows(IllegalStateException.class, () -> createBuilderWithTestModel()
                .baseDirectory(boxStoreDir)
                .inMemory("options-test")
        );
    }

    @Test
    public void inMemoryCreatesNoFiles() {
        // let base class clean up store in tearDown method
        store = createBuilderWithTestModel().inMemory("in-memory-test").build();

        assertFalse(boxStoreDir.exists());
        assertFalse(new File("memory").exists());
        assertFalse(new File("memory:").exists());
        String identifierPart = boxStoreDir.getPath().substring("memory:".length());
        assertFalse(new File(identifierPart).exists());
    }

    @Test
    public void testMaxReaders() {
        builder = createBoxStoreBuilder(null);
        store = builder.maxReaders(1).build();
        final Exception[] exHolder = {null};
        final Thread thread = new Thread(() -> {
            try {
                getTestEntityBox().count();
            } catch (Exception e) {
                exHolder[0] = e;
            }
            getTestEntityBox().closeThreadResources();
        });

        getTestEntityBox().count();
        store.runInReadTx(() -> {
            getTestEntityBox().count();
            thread.start();
            try {
                thread.join(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        // TODO: not working (debugged maxReaders get passed to native OK)
//        assertNotNull(exHolder[0]);
//        assertEquals(DbMaxReadersExceededException.class, exHolder[0].getClass());
    }

    @Test
    public void readOnly() {
        // Create a database first; we must create the model only once (ID/UID sequences would be different 2nd time)
        byte[] model = createTestModel(null);
        builder = new BoxStoreBuilder(model).directory(boxStoreDir);
        store = builder.build();
        store.close();

        // Then re-open database with same model as read-only.
        builder = new BoxStoreBuilder(model).directory(boxStoreDir);
        builder.readOnly();
        store = builder.build();

        assertTrue(store.isReadOnly());
    }

    @Test
    public void maxSize_invalidValues_throw() {
        // Max data larger than max database size throws.
        builder.maxSizeInKByte(10);
        IllegalArgumentException exSmaller = assertThrows(
                IllegalArgumentException.class,
                () -> builder.maxDataSizeInKByte(11)
        );
        assertEquals("maxDataSizeInKByte must be smaller than maxSizeInKByte.", exSmaller.getMessage());

        // Max database size smaller than max data size throws.
        builder.maxDataSizeInKByte(9);
        IllegalArgumentException exLarger = assertThrows(
                IllegalArgumentException.class,
                () -> builder.maxSizeInKByte(8)
        );
        assertEquals("maxSizeInKByte must be larger than maxDataSizeInKByte.", exLarger.getMessage());
    }

    @Test
    public void maxFileSize() {
        assumeFalse(IN_MEMORY); // no max size support for in-memory

        // To avoid frequently changing the limit choose one high enough to insert at least one object successfully,
        // then keep inserting until the limit is hit.
        builder = createBoxStoreBuilder(null);
        builder.maxSizeInKByte(150);
        store = builder.build();

        putTestEntity(LONG_STRING, 1); // Should work

        boolean dbFullExceptionThrown = false;
        for (int i = 2; i < 1000; i++) {
            TestEntity testEntity = createTestEntity(LONG_STRING, i);
            try {
                getTestEntityBox().put(testEntity);
            } catch (DbFullException e) {
                dbFullExceptionThrown = true;
                break;
            }
        }
        assertTrue("DbFullException was not thrown",  dbFullExceptionThrown);

        // Check re-opening with larger size allows to insert again
        store.close();
        builder.maxSizeInKByte(200);
        store = builder.build();
        getTestEntityBox().put(createTestEntity(LONG_STRING, 1000));
    }

    @Test
    public void maxDataSize() {
        // Put until max data size is reached, but still below max database size.
        builder = createBoxStoreBuilder(null);
        builder.maxSizeInKByte(50); // Empty file is around 12 KB, each put adds about 8 KB.
        builder.maxDataSizeInKByte(1);
        store = builder.build();

        TestEntity testEntity1 = putTestEntity(LONG_STRING, 1);
        TestEntity testEntity2 = createTestEntity(LONG_STRING, 2);
        DbMaxDataSizeExceededException maxDataExc = assertThrows(
                DbMaxDataSizeExceededException.class,
                () -> getTestEntityBox().put(testEntity2)
        );
        assertEquals("Exceeded user-set maximum by [bytes]: 560", maxDataExc.getMessage());

        // Remove to get below max data size, then put again.
        getTestEntityBox().remove(testEntity1);
        getTestEntityBox().put(testEntity2);

        // Alternatively, re-open with larger max data size.
        store.close();
        builder.maxDataSizeInKByte(2);
        store = builder.build();
        putTestEntity(LONG_STRING, 3);
    }


    @Test
    public void testCreateClone() {
        builder = createBoxStoreBuilder(null);
        store = builder.build();
        putTestEntity(LONG_STRING, 1);

        BoxStoreBuilder clonedBuilder = builder.createClone("-cloned");
        assertEquals(clonedBuilder.directory.getAbsolutePath(), boxStoreDir.getAbsolutePath() + "-cloned");

        BoxStore clonedStore = clonedBuilder.build();
        assertNotNull(clonedStore);
        assertNotSame(store, clonedStore);
        assertArrayEquals(store.getAllEntityTypeIds(), clonedStore.getAllEntityTypeIds());

        Box<TestEntity> boxOriginal = store.boxFor(TestEntity.class);
        assertEquals(1, boxOriginal.count());
        Box<TestEntity> boxClone = clonedStore.boxFor(TestEntity.class);
        assertEquals(0, boxClone.count());

        boxClone.put(createTestEntity("I'm a clone", 2));
        boxClone.put(createTestEntity("I'm a clone, too", 3));
        assertEquals(2, boxClone.count());
        assertEquals(1, boxOriginal.count());

        store.close();
        clonedStore.close();
        clonedStore.deleteAllFiles();
    }
}
