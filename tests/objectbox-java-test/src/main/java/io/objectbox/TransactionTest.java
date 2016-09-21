package io.objectbox;

import io.objectbox.exception.DbException;
import org.junit.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TransactionTest extends AbstractObjectBoxTest {

    private void prepareOneEntryWith123() {
        // prepare the data
        Transaction transaction = store.beginTx();
        KeyValueCursor cursor = transaction.createKeyValueCursor();
        cursor.put(123, new byte[]{1, 2, 3});
        cursor.close();
        assertEquals(true, transaction.isActive());
        transaction.commit();
        assertEquals(false, transaction.isActive());
    }

    @Test
    public void testTransactionCommitAndAbort() {
        prepareOneEntryWith123();

        Transaction transaction = store.beginTx();
        KeyValueCursor cursor = transaction.createKeyValueCursor();
        cursor.put(123, new byte[]{3, 2, 1});
        assertArrayEquals(new byte[]{3, 2, 1}, cursor.get(123));
        cursor.close();
        transaction.abort();

        transaction = store.beginTx();
        cursor = transaction.createKeyValueCursor();
        assertArrayEquals(new byte[]{1, 2, 3}, cursor.get(123));
        cursor.close();
        transaction.abort();
    }

    @Test
    public void testReadTransactionWhileWriting() {
        prepareOneEntryWith123();

        Transaction txWrite = store.beginTx();
        Transaction txRead = store.beginReadTx();

        // start writing
        KeyValueCursor cursorWrite = txWrite.createKeyValueCursor();
        cursorWrite.put(123, new byte[]{3, 2, 1});
        assertArrayEquals(new byte[]{3, 2, 1}, cursorWrite.get(123));
        cursorWrite.close();

        // start reading the old value
        KeyValueCursor cursorRead = txRead.createKeyValueCursor();
        assertArrayEquals(new byte[]{1, 2, 3}, cursorRead.get(123));
        cursorRead.close();

        // commit writing
        assertEquals(true, txRead.isReadOnly());
        assertEquals(false, txWrite.isReadOnly());

        assertEquals(true, txWrite.isActive());
        txWrite.commit();
        assertEquals(false, txWrite.isActive());

        // commit reading
        assertEquals(true, txRead.isActive());
        txRead.abort();
        assertEquals(false, txRead.isActive());

        // start reading again and get the new value
        txRead = store.beginReadTx();
        cursorRead = txRead.createKeyValueCursor();
        assertArrayEquals(new byte[]{3, 2, 1}, cursorRead.get(123));
        cursorRead.close();

        txRead.abort();

        store.close();
    }

    @Test
    public void testTransactionReset() {
        prepareOneEntryWith123();

        // write transaction

        Transaction transaction = store.beginTx();
        KeyValueCursor cursor = transaction.createKeyValueCursor();
        cursor.put(123, new byte[]{3, 2, 1});
        assertArrayEquals(new byte[]{3, 2, 1}, cursor.get(123));
        cursor.close();
        transaction.reset();
        assertEquals(true, transaction.isActive());

        cursor = transaction.createKeyValueCursor();
        assertArrayEquals(new byte[]{1, 2, 3}, cursor.get(123));
        cursor.close();
        transaction.abort();

        transaction.reset();
        cursor = transaction.createKeyValueCursor();
        cursor.put(123, new byte[]{3, 2, 1});
        assertArrayEquals(new byte[]{3, 2, 1}, cursor.get(123));
        cursor.close();
        transaction.commit();

        transaction.reset();
        cursor = transaction.createKeyValueCursor();
        assertArrayEquals(new byte[]{3, 2, 1}, cursor.get(123));
        cursor.close();
        transaction.commit();

        // read transaction

        transaction = store.beginReadTx();
        cursor = transaction.createKeyValueCursor();
        assertArrayEquals(new byte[]{3, 2, 1}, cursor.get(123));
        cursor.close();
        transaction.reset();
        assertEquals(true, transaction.isActive());

        cursor = transaction.createKeyValueCursor();
        assertArrayEquals(new byte[]{3, 2, 1}, cursor.get(123));
        cursor.close();
        transaction.abort();
    }

    @Test(expected = DbException.class)
    public void testCreateCursorAfterAbortException() {
        Transaction tx = store.beginReadTx();
        tx.abort();
        tx.createKeyValueCursor();
    }

    @Test(expected = DbException.class)
    public void testCommitAfterAbortException() {
        Transaction tx = store.beginTx();
        tx.abort();
        tx.commit();
    }

    @Test(expected = DbException.class)
    public void testCommitReadTxException() {
        Transaction tx = store.beginReadTx();
        tx.commit();
    }

    /*
    @Test
    public void testTransactionUsingAfterStoreClosed() {
        prepareOneEntryWith123();

        // write transaction
        Transaction transaction = store.beginTx();
        Cursor cursor = transaction.createCursor();

        store.close();

        cursor.put(123, new byte[]{3, 2, 1});
        assertArrayEquals(new byte[]{3, 2, 1}, cursor.get(123));
        cursor.close();
        transaction.reset();
        assertEquals(true, transaction.isActive());
    }*/

    @Test
    public void testTxGC() throws InterruptedException {
        // For a real test, use count = 100000 and check console output that TX get freed in between
        int count = runExtensiveTests ? 100000 : 1000;
        Thread[] threads = new Thread[count];
        final AtomicInteger threadsOK = new AtomicInteger();
        final AtomicInteger readersFull = new AtomicInteger();
        for (int i = 0; i < count; i++) {
            threads[i] = new Thread() {
                @Override
                public void run() {
                    try {
                        Transaction tx = store.beginReadTx();
                        //tx.createKeyValueCursor();
                    } catch (DbException e) {
                        if (e.getErrorCode() == -30790 || // "MDB_READERS_FULL"
                                "Maximum of threads/transactions reached".equals(e.getMessage())) {
                            readersFull.incrementAndGet();
                        } else {
                            throw e;
                        }
                    }
                    threadsOK.incrementAndGet();
                }
            };
            threads[i].start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        System.out.println("MDB_READERS_FULL: " + readersFull);

        assertEquals(count, threadsOK.get());
    }

    @Test
    public void testClose() {
        Transaction tx = store.beginReadTx();
        assertFalse(tx.isClosed());
        tx.close();
        assertTrue(tx.isClosed());

        // Double close should be fine
        tx.close();

        try {
            tx.reset();
            fail("Should have thrown");
        } catch (IllegalStateException e) {
            // OK
        }
    }

    @Test
    public void testRunInTxRecursive() {
        final Box<TestEntity> box = getTestEntityBox();
        final long[] counts = {0, 0, 0};
        store.runInTx(new Runnable() {
            @Override
            public void run() {
                box.put(new TestEntity());
                counts[0] = box.count();
                try {
                    store.callInTx(new Callable<Void>() {
                        @Override
                        public Void call() {
                            store.runInTx(new Runnable() {
                                @Override
                                public void run() {
                                    box.put(new TestEntity());
                                    counts[1] = box.count();
                                }
                            });
                            box.put(new TestEntity());
                            counts[2] = box.count();
                            return null;
                        }

                    });
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
        assertEquals(1, counts[0]);
        assertEquals(2, counts[1]);
        assertEquals(3, counts[2]);
        assertEquals(3, box.count());
    }

    @Test
    public void testCallInTxAsync_multiThreaded() throws InterruptedException {
        final Box<TestEntity> box = getTestEntityBox();
        final Thread mainTestThread = Thread.currentThread();
        final AtomicInteger number = new AtomicInteger();
        final AtomicInteger errorCount = new AtomicInteger();
        final int countThreads = runExtensiveTests ? 500 : 10;
        final int countEntities = runExtensiveTests ? 1000 : 100;
        final CountDownLatch threadsDoneLatch = new CountDownLatch(countThreads);

        Callable<Object> callable = new Callable<Object>() {
            @Override
            public Long call() throws Exception {
                assertNotSame(mainTestThread, Thread.currentThread());
                for (int i = 0; i < countEntities; i++) {
                    TestEntity entity = new TestEntity();
                    final int value = number.incrementAndGet();
                    entity.setSimpleInt(value);
                    long key = box.put(entity);
                    TestEntity read = box.get(key);
                    assertEquals(value, read.getSimpleInt());
                }
                return box.count();
            }
        };
        TxCallback<Object> callback = new TxCallback<Object>() {
            @Override
            public void txFinished(Object result, Throwable error) {
                threadsDoneLatch.countDown();
                if (error != null) {
                    errorCount.incrementAndGet();
                    error.printStackTrace();
                }
            }
        };
        for (int i = 0; i < countThreads; i++) {
            store.callInTxAsync(callable, callback);
        }
        assertTrue(threadsDoneLatch.await(runExtensiveTests ? 120 : 5, TimeUnit.SECONDS));
        assertEquals(countThreads * countEntities, number.get());
        assertEquals(countThreads * countEntities, box.count());
        assertEquals(0, errorCount.get());
    }


}