package io.objectbox;

import org.junit.Test;

import java.io.File;

import io.objectbox.exception.DbException;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class BoxStoreTest extends AbstractObjectBoxTest {

    @Test
    public void testUnalignedMemoryAccess() {
        BoxStore.testUnalignedMemoryAccess();
    }

    @Test
    public void testClose() {
        assertFalse(store.isClosed());
        store.close();
        assertTrue(store.isClosed());

        // Double close should be fine
        store.close();
    }

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

    @Test(expected = DbException.class)
    public void testPreventTwoBoxStoresWithSameFileOpenend() {
        createBoxStore();
    }

    @Test
    public void testOpenSameBoxStoreAfterClose() {
        store.close();
        createBoxStore();
    }

    @Test
    public void testOpenTwoBoxStoreTwoFiles() {
        File boxStoreDir2 = new File(boxStoreDir.getAbsolutePath() + "-2");
        BoxStoreBuilder builder = new BoxStoreBuilder(createTestModel(false)).directory(boxStoreDir2);
        builder.entity(new TestEntity_());
    }

}