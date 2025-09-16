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

import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import java.io.File;
import java.util.concurrent.Callable;
import java.util.concurrent.RejectedExecutionException;

import io.objectbox.exception.DbException;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeFalse;

public class BoxStoreTest extends AbstractObjectBoxTest {

    @Test
    public void testEmptyTransaction() {
        Transaction transaction = store.beginTx();
        transaction.commit();
    }

    @Test
    public void testSameBox() {
        Box<TestEntity> box1 = store.boxFor(TestEntity.class);
        Box<TestEntity> box2 = store.boxFor(TestEntity.class);
        assertSame(box1, box2);
    }

    @Test(expected = RuntimeException.class)
    public void testBoxForUnknownEntity() {
        store.boxFor(getClass());
    }

    @Test
    public void testRegistration() {
        assertEquals("TestEntity", store.getDbName(TestEntity.class));
        assertEquals(TestEntity.class, store.getEntityInfo(TestEntity.class).getEntityClass());
    }

    @Test
    public void testCloseThreadResources() {
        Box<TestEntity> box = store.boxFor(TestEntity.class);
        Cursor<TestEntity> reader = box.getReader();
        box.releaseReader(reader);

        Cursor<TestEntity> reader2 = box.getReader();
        box.releaseReader(reader2);
        assertSame(reader, reader2);

        store.closeThreadResources();
        Cursor<TestEntity> reader3 = box.getReader();
        box.releaseReader(reader3);
        assertNotSame(reader, reader3);
    }

    @Test
    public void testClose() {
        // This test suite uses a single entity (TestEntity) by default
        // and all other tests close the store after being done. So should be 1.
        assertEquals(1, BoxStore.nativeGloballyActiveEntityTypes());

        BoxStore store = this.store;
        assertFalse(store.isClosed());
        store.close();
        assertTrue(store.isClosed());
        // Assert native Entity instances are not leaked.
        assertEquals(0, BoxStore.nativeGloballyActiveEntityTypes());

        // Double close should be fine
        store.close();

        // Internal thread pool is shut down.
        assertTrue(store.internalThreadPool().isShutdown());
        assertTrue(store.internalThreadPool().isTerminated());

        // Can still obtain a box (but not use it).
        store.boxFor(TestEntity.class);
        store.closeThreadResources();
        //noinspection ResultOfMethodCallIgnored
        store.getObjectBrowserPort();
        store.isObjectBrowserRunning();
        //noinspection ResultOfMethodCallIgnored
        store.isDebugRelations();
        store.internalQueryAttempts();
        store.internalFailedReadTxAttemptCallback();
        //noinspection ResultOfMethodCallIgnored
        store.getSyncClient();
        store.setSyncClient(null);

        // Methods using the native store should throw.
        assertThrowsStoreIsClosed(store::getDbSize);
        assertThrowsStoreIsClosed(store::getDbSizeOnDisk);
        assertThrowsStoreIsClosed(store::beginTx);
        assertThrowsStoreIsClosed(store::beginReadTx);
        assertThrowsStoreIsClosed(store::isReadOnly);
        assertThrowsStoreIsClosed(store::removeAllObjects);
        assertThrowsStoreIsClosed(() -> store.runInTx(() -> {
        }));
        assertThrowsStoreIsClosed(() -> store.runInReadTx(() -> {
        }));
        assertThrowsStoreIsClosed(() -> store.callInReadTxWithRetry(() -> null,
                3, 1, true));
        assertThrowsStoreIsClosed(() -> store.callInReadTx(() -> null));
        assertThrowsStoreIsClosed(() -> store.callInTx(() -> null));
        // callInTxNoException wraps in RuntimeException
        RuntimeException runtimeException = assertThrows(RuntimeException.class, () -> store.callInTxNoException(() -> null));
        assertEquals("java.lang.IllegalStateException: Store is closed", runtimeException.getMessage());
        // Internal thread pool is shut down as part of closing store, should no longer accept new work.
        assertThrows(RejectedExecutionException.class, () -> store.runInTxAsync(() -> {}, null));
        assertThrows(RejectedExecutionException.class, () -> store.callInTxAsync(() -> null, null));
        assertThrowsStoreIsClosed(store::diagnose);
        assertThrowsStoreIsClosed(() -> store.validate(0, false));
        assertThrowsStoreIsClosed(store::cleanStaleReadTransactions);
        assertThrowsStoreIsClosed(store::subscribe);
        assertThrowsStoreIsClosed(() -> store.subscribe(TestEntity.class));
        assertThrowsStoreIsClosed(store::startObjectBrowser);
        assertThrowsStoreIsClosed(() -> store.startObjectBrowser(12345));
        assertThrowsStoreIsClosed(() -> store.startObjectBrowser("http://127.0.0.1"));
        // assertThrowsStoreIsClosed(store::stopObjectBrowser); // Requires mocking, not testing for now.
        assertThrowsStoreIsClosed(() -> store.setDbExceptionListener(null));
        // Internal thread pool is shut down as part of closing store, should no longer accept new work.
        assertThrows(RejectedExecutionException.class, () -> store.internalScheduleThread(() -> {}));
        assertThrowsStoreIsClosed(() -> store.setDebugFlags(0));
        assertThrowsStoreIsClosed(() -> store.panicModeRemoveAllObjects(TestEntity_.__ENTITY_ID));
        assertThrowsStoreIsClosed(store::getNativeStore);
    }

    private void assertThrowsStoreIsClosed(ThrowingRunnable runnable) {
        IllegalStateException ex = assertThrows(IllegalStateException.class, runnable);
        assertEquals("Store is closed", ex.getMessage());
    }

    @Test
    public void openSamePath_fails() {
        DbException ex = assertThrows(DbException.class, this::createBoxStore);
        assertTrue(ex.getMessage().contains("Another BoxStore is still open for this directory"));
    }

    @Test
    public void openSamePath_afterClose_works() {
        store.close();
        // Assert native Entity instances are not leaked.
        assertEquals(0, BoxStore.nativeGloballyActiveEntityTypes());

        BoxStore store2 = createBoxStore();
        store2.close();
        // Assert native Entity instances are not leaked.
        assertEquals(0, BoxStore.nativeGloballyActiveEntityTypes());
    }

    @Test
    public void openSamePath_closedByFinalizer_works() {
        System.out.println("Removing reference to " + store);
        store = null;

        // When another Store is still open using the same path, a checker thread is started that periodically triggers
        // garbage collection and finalization in the VM, which here should close store and allow store2 to be opened.
        // Note that user code should not rely on this, see notes on BoxStore.finalize().
        BoxStore store2 = createBoxStore();
        store2.close();
        System.out.println("Closed " + store2);
    }

    @Test
    public void testOpenTwoBoxStoreTwoFiles() {
        File boxStoreDir2 = new File(boxStoreDir.getAbsolutePath() + "-2");
        BoxStoreBuilder builder = createBuilderWithTestModel().directory(boxStoreDir2);
        builder.entity(new TestEntity_());
    }

    @Test
    public void testDeleteAllFiles() {
        // Note: for in-memory can not really assert database is gone,
        // e.g. using sizeOnDisk is not possible after closing the store from Java.
        closeStoreForTest();
    }

    @Test
    public void testDeleteAllFiles_staticDir() {
        assumeFalse(IN_MEMORY);
        closeStoreForTest();

        File boxStoreDir2 = new File(boxStoreDir.getAbsolutePath() + "-2");
        BoxStoreBuilder builder = createBuilderWithTestModel().directory(boxStoreDir2);
        BoxStore store2 = builder.build();
        store2.close();

        assertTrue(boxStoreDir2.exists());
        assertTrue(BoxStore.deleteAllFiles(boxStoreDir2));
        assertFalse(boxStoreDir2.exists());
    }

    @Test
    public void testDeleteAllFiles_baseDirName() {
        assumeFalse(IN_MEMORY);

        closeStoreForTest();
        File basedir = new File("test-base-dir");
        String name = "mydb";
        if (!basedir.exists()) {
            if (!basedir.mkdir()) {
                fail("Failed to create test directory.");
            }
        }
        assertTrue(basedir.isDirectory());
        File dbDir = new File(basedir, name);
        assertFalse(dbDir.exists());

        BoxStoreBuilder builder = createBuilderWithTestModel().baseDirectory(basedir).name(name);
        BoxStore store2 = builder.build();
        store2.close();

        assertTrue(dbDir.exists());
        assertTrue(BoxStore.deleteAllFiles(basedir, name));
        assertFalse(dbDir.exists());
        assertTrue(basedir.delete());
    }

    @Test(expected = IllegalStateException.class)
    public void testDeleteAllFiles_openStore() {
        BoxStore.deleteAllFiles(boxStoreDir);
    }

    @Test
    public void removeAllObjects() {
        // Insert at least two different kinds.
        store.close();
        store.deleteAllFiles();
        store = createBoxStoreBuilderWithTwoEntities(false).build();
        putTestEntities(5);
        Box<TestEntityMinimal> minimalBox = store.boxFor(TestEntityMinimal.class);
        minimalBox.put(new TestEntityMinimal(0, "Sally"));
        assertEquals(5, getTestEntityBox().count());
        assertEquals(1, minimalBox.count());

        store.removeAllObjects();
        assertEquals(0, getTestEntityBox().count());
        assertEquals(0, minimalBox.count());

        // Assert inserting is still possible.
        putTestEntities(1);
        assertEquals(1, getTestEntityBox().count());
    }

    private void closeStoreForTest() {
        if (!IN_MEMORY) {
            assertTrue(boxStoreDir.exists());
        }
        store.close();
        assertTrue(store.deleteAllFiles());
        assertFalse(boxStoreDir.exists());
    }

    @Test
    public void testCallInReadTxWithRetry() {
        final int[] countHolder = {0};
        String value = store.callInReadTxWithRetry(createTestCallable(countHolder), 5, 0, true);
        assertEquals("42", value);
        assertEquals(5, countHolder[0]);
    }

    @Test(expected = DbException.class)
    public void testCallInReadTxWithRetry_fail() {
        final int[] countHolder = {0};
        store.callInReadTxWithRetry(createTestCallable(countHolder), 4, 0, true);
    }

    @Test
    public void testCallInReadTxWithRetry_callback() {
        closeStoreForTest();
        final int[] countHolder = {0};
        final int[] countHolderCallback = {0};

        BoxStoreBuilder builder = createBuilderWithTestModel().directory(boxStoreDir)
                .failedReadTxAttemptCallback((result, error) -> {
                    assertNotNull(error);
                    countHolderCallback[0]++;
                });
        store = builder.build();
        String value = store.callInReadTxWithRetry(createTestCallable(countHolder), 5, 0, true);
        assertEquals("42", value);
        assertEquals(5, countHolder[0]);
        assertEquals(4, countHolderCallback[0]);
    }

    private Callable<String> createTestCallable(final int[] countHolder) {
        return () -> {
            int count = ++countHolder[0];
            if (count < 5) {
                throw new DbException("This exception IS expected. Count: " + count);
            }
            return "42";
        };
    }

    @Test
    public void testSizeOnDisk() {
        // Note: initial database does have a non-zero (file) size.
        assertTrue(store.getDbSize() > 0);

        long sizeOnDisk = store.getDbSizeOnDisk();
        // Check the file size is at least a reasonable value
        assertTrue("Size is not reasonable", IN_MEMORY ? sizeOnDisk == 0 : sizeOnDisk > 10000 /* 10 KB */);

        // Check the file size increases after inserting
        putTestEntities(10);
        long sizeOnDiskAfterPut = store.getDbSizeOnDisk();
        assertTrue("Size did not increase", IN_MEMORY ? sizeOnDiskAfterPut == 0 : sizeOnDiskAfterPut > sizeOnDisk);
    }

    @Test
    public void validate() {
        putTestEntities(100);

        // Note: not implemented for in-memory, returns 0.
        // No limit.
        long validated = store.validate(0, true);
        assertTrue(IN_MEMORY ? validated == 0 : validated > 2 /* must be larger than with pageLimit == 1, see below */);

        // With limit.
        validated = store.validate(1, true);
        // 2 because the first page doesn't contain any actual data?
        assertEquals(IN_MEMORY ? 0 : 2, validated);
    }

    @Test
    public void testIsObjectBrowserAvailable() {
        assertFalse(BoxStore.isObjectBrowserAvailable());
    }

    @Test
    public void testSysProc() {
        long vmRss = BoxStore.sysProcStatusKb("VmRSS");
        long memAvailable = BoxStore.sysProcMeminfoKb("MemAvailable");

        final String osName = System.getProperty("os.name");
        if (osName.toLowerCase().contains("linux")) {
            System.out.println("VmRSS: " + vmRss);
            System.out.println("MemAvailable: " + memAvailable);
            assertTrue(vmRss > 0);
            assertTrue(memAvailable > 0);
        } else {
            assertEquals(0, vmRss);
            assertEquals(0, memAvailable);
        }
    }

}