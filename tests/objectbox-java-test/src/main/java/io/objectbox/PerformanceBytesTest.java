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

import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.Random;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PerformanceBytesTest extends AbstractObjectBoxTest {

    protected BoxStore createBoxStore() {
        // We need more space
        BoxStoreBuilder builder = createBoxStoreBuilder(false);
        BoxStore boxStore = builder.maxSizeInKByte(100 * 1024).build();
        //boxStore.dropAllData();
        return boxStore;
    }

    @Test
    public void testPutAndGet0Bytes() {
        testPutAndGetBytes(10000, 0);
    }

    @Test
    @Ignore(value = "Currently, size must be multiple of 4 for native")
    public void testPutAndGet1Byte() {
        testPutAndGetBytes(10000, 1);
    }

    @Test
    @Ignore(value = "Currently, size must be multiple of 4 for native")
    public void testPutAndGet10Bytes() {
        testPutAndGetBytes(10000, 10);
    }

    @Test
    public void testPutAndGet100Bytes() {
        testPutAndGetBytes(10000, 100);
    }

    @Test
    public void testPutAndGet1000Bytes() {
        testPutAndGetBytes(10000, 1000);
    }

    private void testPutAndGetBytes(int count, int valueSize) {
        byte[][] byteArrays = createRandomBytes(count, valueSize);

        long start = time();
        Transaction transaction = store.beginTx();
        KeyValueCursor cursor = transaction.createKeyValueCursor();
        for (int key = 1; key <= count; key++) {
            cursor.put(key, byteArrays[key - 1]);
        }
        cursor.close();
        transaction.commitAndClose();
        long time = time() - start;
        log("Wrote " + count + " values with size " + valueSize + " 1-by-1: " + time + "ms, " + valuesPerSec(count, time) + " values/s");

        byte[][] byteArraysRead = new byte[count][valueSize];
        start = time();
        transaction = store.beginTx();
        cursor = transaction.createKeyValueCursor();
        for (int key = 1; key <= count; key++) {
            byteArraysRead[key - 1] = cursor.get(key);
        }
        cursor.close();
        transaction.close();
        time = time() - start;
        log("Read " + count + " values with size " + valueSize + " 1-by-1: " + time + "ms, " + valuesPerSec(count, time) + " values/s");

        for (int i = 0; i < count; i++) {
            assertTrue(Arrays.equals(byteArrays[i], byteArraysRead[i]));
        }
    }

    private byte[][] createRandomBytes(int count, int valueSize) {
        byte[][] byteArrays = new byte[count][valueSize];
        assertEquals(count, byteArrays.length);
        assertEquals(valueSize, byteArrays[0].length);
        Random random = new Random();
        for (byte[] byteArray : byteArrays) {
            random.nextBytes(byteArray);
        }
        return byteArrays;
    }

    private void testCursorAppendAndGetPerformance(int count, int valueSize) {
        byte[][] byteArrays = putBytes(count, valueSize);

        byte[][] byteArraysRead = new byte[count][0];
        long start = time();
        Transaction transaction = store.beginTx();
        KeyValueCursor cursor = transaction.createKeyValueCursor();
        for (int key = 1; key <= count; key++) {
            byteArraysRead[key - 1] = key == 1 ? cursor.get(1) : cursor.getNext();
        }
        cursor.close();
        transaction.close();
        long time = time() - start;
        log("Read " + count + " values with size " + valueSize + " with cursor: " + time + "ms, " + valuesPerSec(count, time));

        for (int i = 0; i < count; i++) {
            String message = "Iteration " + i;
            if (valueSize > 0) assertEquals(message, byteArrays[i][0], byteArraysRead[i][0]);
            assertTrue(message, Arrays.equals(byteArrays[i], byteArraysRead[i]));
        }
    }

    @Test
    public void testCursorAppendAndGetPerformance100() {
        int count = 10000;
        int valueSize = 100;
        testCursorAppendAndGetPerformance(count, valueSize);
    }

    @Test
    public void testCursorAppendAndGetPerformance0() {
        int count = 10000;
        int valueSize = 0;
        testCursorAppendAndGetPerformance(count, valueSize);
    }

    @Test
    public void testCursorAppendAndGetPerformance1000() {
        int count = 10000;
        int valueSize = 1000;
        testCursorAppendAndGetPerformance(count, valueSize);
    }

    private byte[][] putBytes(int count, int valueSize) {
        byte[][] byteArrays = new byte[count][valueSize];
        assertEquals(count, byteArrays.length);
        assertEquals(valueSize, byteArrays[0].length);
        for (byte[] byteArray : byteArrays) {
            random.nextBytes(byteArray);
        }

        long start = time();
        Transaction transaction = store.beginTx();
        KeyValueCursor cursor = transaction.createKeyValueCursor();
        for (int key = 1; key <= count; key++) {
            // TODO does not use append here anymore because append conflicts somehow with the own
            // db mode of the index. having a own db handle puts a key and then the first 0 key is not the lowest
            // anymore -> boom
            cursor.put(key, byteArrays[key - 1]);
        }
        cursor.close();
        transaction.commitAndClose();
        long time = time() - start;
        log("Wrote " + count + " new values with cursor: " + time + "ms, " + valuesPerSec(count, time));
        return byteArrays;
    }

    @Test
    public void testCursorPutTransactionPerformance() {
        int txCount = 1000;
        int valueSize = 300;
        int entryCount = 10;
        byte[][] byteArrays = createRandomBytes(txCount * entryCount, valueSize);
        assertEquals(valueSize, byteArrays[0].length);

        log("Starting " + txCount + " put transactions with " + entryCount + " entries");
        long start = time();
        for (int txNr = 0; txNr < txCount; txNr++) {
            Transaction transaction = store.beginTx();
            KeyValueCursor cursor = transaction.createKeyValueCursor();
            for (int entryNr = 1; entryNr <= entryCount; entryNr++) {
                cursor.put(txNr * entryCount + entryNr, byteArrays[txNr]);
            }

            // TODO use mdb_cursor_renew
            cursor.close();
            transaction.commitAndClose();
        }
        long time = time() - start;
        log("Did " + txCount + " put transactions: " + time + "ms, " + valuesPerSec(txCount, time) + " (TX/s)");
    }

    @Test
    public void testBulkLoadPut() {
        int count = 100000;

        byte[] buffer = new byte[32];
        random.nextBytes(buffer);

        long start = time();
        Transaction transaction = store.beginTx();
        KeyValueCursor cursor = transaction.createKeyValueCursor();
        for (int key = 1; key <= count; key++) {
            cursor.put(key, buffer);
        }
        cursor.close();
        transaction.commitAndClose();

        long time = time() - start;
        log("Bulk load put " + count + " buffers " + time + " ms, " + valuesPerSec(count, time));
    }

    private String valuesPerSec(int count, long timeMillis) {
        return (timeMillis > 0 ? (count * 1000 / timeMillis) : "N/A") + " values/s";
    }

}
