package io.objectbox;

import org.junit.Test;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class BoxStoreTest extends AbstractObjectBoxTest {

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

    @Test
    public void testRegistration() {
        assertEquals("TestEntity", store.getEntityName(TestEntity.class));
        assertEquals(TestEntityCursor.class, store.getEntityCursorClass(TestEntity.class));
    }

}