/*
 * Copyright 2017-2024 ObjectBox Ltd.
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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import io.objectbox.annotation.IndexType;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class CursorTest extends AbstractObjectBoxTest {

    @Override
    protected BoxStore createBoxStore() {
        // Note: can not use DEFAULT as tests use deprecated cursor.lookupKeyUsingIndex method
        // which expects a value based index.
        return createBoxStore(IndexType.VALUE);
    }

    @Test
    public void testPutAndGetEntity() {
        TestEntity entity = new TestEntity();
        entity.setSimpleInt(1977);
        Transaction transaction = store.beginTx();
        Cursor<TestEntity> cursor = transaction.createCursor(TestEntity.class);
        long key = cursor.put(entity);

        TestEntity entityRead = cursor.get(key);
        assertNotNull(entityRead);
        assertEquals(1977, entityRead.getSimpleInt());

        cursor.close();
        transaction.abort();
    }

    @Test
    public void testPutEntityWithInvalidId() {
        TestEntity entity = new TestEntity();
        entity.setId(777);
        Transaction transaction = store.beginTx();
        Cursor<TestEntity> cursor = transaction.createCursor(TestEntity.class);

        try {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> cursor.put(entity));
            assertEquals(ex.getMessage(), "ID is higher or equal to internal ID sequence: 777 (vs. 1)." +
                    " Use ID 0 (zero) to insert new objects.");
        } finally {
            // Always clean up, even if assertions fail, to avoid misleading clean-up errors.
            cursor.close();
            transaction.close();
        }
    }

    @Test
    public void testGetNextEntity() {
        insertTestEntities("hello", "bye", "dummy");
        Transaction transaction = store.beginReadTx();
        Cursor<TestEntity> cursor = transaction.createCursor(TestEntity.class);
        TestEntity entity1 = cursor.get(1);
        assertEquals("hello", entity1.getSimpleString());
        TestEntity entity2 = cursor.next();
        assertEquals("bye", entity2.getSimpleString());
        TestEntity entity3 = cursor.next();
        assertEquals("dummy", entity3.getSimpleString());
        assertNull(cursor.next());
        cursor.close();
        transaction.abort();
    }

    // TODO split up into several test cases
    @Test
    public void testPutGetUpdateDeleteEntity() {

        // create an entity
        TestEntity entity = new TestEntity();
        entity.setSimpleInt(1977);
        entity.setSimpleLong(54321);
        String value1 = "lulu321";
        entity.setSimpleString(value1);
        Transaction transaction = store.beginTx();
        Cursor<TestEntity> cursor = transaction.createCursor(TestEntity.class);
        long key = cursor.put(entity);

        // get it
        TestEntity entityRead = cursor.get(key);
        assertNotNull(entityRead);
        assertEquals(1977, entityRead.getSimpleInt());
        assertEquals(54321, entityRead.getSimpleLong());
        assertEquals(value1, entityRead.getSimpleString());

        // and find via index
        assertEquals(key, cursor.lookupKeyUsingIndex(9, value1));
//        assertEquals(key, cursor.find(TestEntity_.simpleString, value1).get(0).getId());

        // change entity values
        String value2 = "lala123";
        entityRead.setSimpleString(value2);
        entityRead.setSimpleLong(12345);

        // and replace the old one with the new one
        cursor.put(entityRead);

        // indexes ok?
//        assertEquals(0, cursor.find(TestEntity_.simpleString, value1).size());
        assertEquals(0, cursor.lookupKeyUsingIndex(9, value1));

//        assertEquals(key, cursor.find(TestEntity_.simpleString, value2).get(0).getId());

        // get the changed entity
        entityRead = cursor.get(key);
        assertNotNull(entityRead);

        // and check if the values changed
        assertEquals(1977, entityRead.getSimpleInt());
        assertEquals(12345, entityRead.getSimpleLong());
        assertEquals(value2, entityRead.getSimpleString());

        // and remove it
        cursor.deleteEntity(key);

        // not in any index anymore
//        assertEquals(0, cursor.find(TestEntity_.simpleString, value1).size());
//        assertEquals(0, cursor.find(TestEntity_.simpleString, value2).size());

        cursor.close();
        transaction.abort();
    }

    @Test
    public void testPutSameIndexValue() {
        TestEntity entity = new TestEntity();
        String value = "lulu321";
        entity.setSimpleString(value);
        Transaction transaction = store.beginTx();
        TestEntity read;
        try {
            Cursor<TestEntity> cursor = transaction.createCursor(TestEntity.class);
            long key = cursor.put(entity);
            // And again
            entity.setSimpleInt(1977);
            cursor.put(entity);
            assertEquals(key, cursor.lookupKeyUsingIndex(9, value));
            read = cursor.get(key);
            cursor.close();
        } finally {
            transaction.close();
        }
        assertEquals(1977, read.getSimpleInt());
        assertEquals(value, read.getSimpleString());
    }

    private void insertTestEntities(String... texts) {
        Transaction transaction = store.beginTx();
        Cursor<TestEntity> cursor = transaction.createCursor(TestEntity.class);
        for (String text : texts) {
            putEntity(cursor, text, 0);
        }
        cursor.close();
        transaction.commitAndClose();
    }

    @Test
    public void testLookupKeyUsingIndex() {
        insertTestEntities("find me", "not me");

        Transaction transaction = store.beginTx();
        Cursor<TestEntity> cursor = transaction.createCursor(TestEntity.class);

        assertEquals(2, cursor.lookupKeyUsingIndex(9, "not me"));
        assertEquals(1, cursor.lookupKeyUsingIndex(9, "find me"));
        assertEquals(0, cursor.lookupKeyUsingIndex(9, "peter pan"));

        cursor.close();
        transaction.abort();
    }

    @Test
    public void testLookupKeyUsingIndex_samePrefix() {
        insertTestEntities("aaa", "aa");

        Transaction transaction = store.beginTx();
        Cursor<TestEntity> cursor = transaction.createCursor(TestEntity.class);

        assertEquals(0, cursor.lookupKeyUsingIndex(9, "a"));
        assertEquals(2, cursor.lookupKeyUsingIndex(9, "aa"));
        assertEquals(1, cursor.lookupKeyUsingIndex(9, "aaa"));
        assertEquals(0, cursor.lookupKeyUsingIndex(9, "aaaa"));

        cursor.close();
        transaction.abort();
    }

    @Test
    public void testClose() {
        try (Transaction tx = store.beginReadTx()) {
            Cursor<TestEntity> cursor = tx.createCursor(TestEntity.class);
            assertFalse(cursor.isClosed());
            cursor.close();
            assertTrue(cursor.isClosed());

            // Double close should be fine
            cursor.close();
        }
    }

    /**
     * Begin the first write-TX and ensure the second one blocks until the first one is closed.
     * A secondary test goal is to check races of a closing TX and a closing store.
     */
    @Test
    public void testWriteTxBlocksOtherWriteTx() throws InterruptedException {
        // To change the likelihood of the TX vs store closing race, close the store using one of 3 different variants.
        // Assign and print the randomly chosen variant beforehand so it does not mess with thread timings later.
        // Warning: test variant 2 only manually, it will close the write-TX from a non-owner thread (in BoxStore.close)
        // where the native database is expected to panic.
        int closeStoreVariant = ThreadLocalRandom.current().nextInt(2 /* 3 - test variant 2 manually, see above */);
        System.out.println("Closing store variant: " + closeStoreVariant);

        long time = System.currentTimeMillis();
        Transaction tx = store.beginTx();
        long duration = System.currentTimeMillis() - time; // Usually 0 on desktop
        final CountDownLatch latchBeforeBeginTx = new CountDownLatch(1);
        final CountDownLatch latchAfterBeginTx = new CountDownLatch(1);
        final CountDownLatch latchCloseStoreInMainThread = closeStoreVariant != 0 ? new CountDownLatch(1) : null;
        new Thread(() -> {
            latchBeforeBeginTx.countDown();
            Transaction tx2 = store.beginTx();
            latchAfterBeginTx.countDown();

            if (closeStoreVariant != 0) {
                try {
                    assertTrue(latchCloseStoreInMainThread.await(5, TimeUnit.SECONDS));
                } catch (InterruptedException e) {
                    throw new RuntimeException("Interrupted", e);
                }
            }
            tx2.close();
        }).start();
        assertTrue(latchBeforeBeginTx.await(1, TimeUnit.SECONDS));
        long waitTime = 100 + duration * 10;
        assertFalse(latchAfterBeginTx.await(waitTime, TimeUnit.MILLISECONDS));
        tx.close();
        assertTrue(latchAfterBeginTx.await(waitTime * 2, TimeUnit.MILLISECONDS));

        // closeStoreVariant == 0: not latch waiting, close store when tearing down test
        if (closeStoreVariant == 1) {
            // This variant tries to close the store and the TX at the same time.
            latchCloseStoreInMainThread.countDown();
            store.close();  // Maybe this runs a tiny bit before the close() in teardown(?)
        } else if (closeStoreVariant == 2) {
            store.close();  // Enforces closing the store before the TX.
            latchCloseStoreInMainThread.countDown();
        }
    }

    @Test
    public void testGetPropertyId() {
        Transaction transaction = store.beginReadTx();
        Cursor<TestEntity> cursor = transaction.createCursor(TestEntity.class);
        assertEquals(1, cursor.getPropertyId("id"));
        assertEquals(2, cursor.getPropertyId("simpleBoolean"));
        assertEquals(3, cursor.getPropertyId("simpleByte"));
        assertEquals(4, cursor.getPropertyId("simpleShort"));
        cursor.close();
        transaction.abort();
    }

    @Test
    public void testRenew() {
        insertTestEntities("orange");

        Transaction transaction = store.beginReadTx();
        Cursor<TestEntity> cursor = transaction.createCursor(TestEntity.class);
        transaction.recycle();
        transaction.renew();
        cursor.renew();
        assertEquals("orange", cursor.get(1).getSimpleString());

        cursor.close();
        transaction.close();
    }

    @Test
    public void testThrowInEntityConstructor() {
        insertTestEntities(TestEntity.STRING_VALUE_THROW_IN_CONSTRUCTOR);

        Transaction transaction = store.beginReadTx();
        Cursor<TestEntity> cursor = transaction.createCursor(TestEntity.class);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> cursor.get(1)
        );
        assertEquals(TestEntity.EXCEPTION_IN_CONSTRUCTOR_MESSAGE, exception.getMessage());

        cursor.close();
        transaction.close();
    }

    private TestEntity putEntity(Cursor<TestEntity> cursor, String text, int number) {
        TestEntity entity = new TestEntity();
        entity.setSimpleString(text);
        entity.setSimpleInt(number);
        cursor.put(entity);
        return entity;
    }

}
