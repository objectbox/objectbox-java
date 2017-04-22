package io.objectbox;

import org.junit.Test;


import io.objectbox.internal.CallWithHandle;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
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
    // FIXME test is flaky
    public void testCloseThreadResources() {
        Box<TestEntity> box = store.boxFor(TestEntity.class);
        long internalHandle = getInternalReaderHandle(box);
        assertTrue(internalHandle != 0);
        long internalHandle2 = getInternalReaderHandle(box);
        assertEquals(internalHandle, internalHandle2);

        store.closeThreadResources();
        long internalHandle3 = getInternalReaderHandle(box);
        assertNotEquals(internalHandle, internalHandle3);
    }

    private long getInternalReaderHandle(Box<TestEntity> box) {
        final long[] handleRef = {0};
        box.internalCallWithReaderHandle(new CallWithHandle<Void>() {
            @Override
            public Void call(long handle) {
                handleRef[0] = handle;
                return null;
            }
        });
        return handleRef[0];
    }

}