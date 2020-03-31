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

import org.junit.Test;

import java.util.Arrays;
import java.util.Random;

import static org.junit.Assert.*;

// NOTE: Sizes must be multiple of 4 (currently not enforced)
public class CursorBytesTest extends AbstractObjectBoxTest {
    static final boolean EXTENSIVE = false;

    @Test
    public void testPutAndGet() {
        byte[] value = {23, 27, 42, 66};
        Transaction transaction = store.beginTx();
        KeyValueCursor cursor = transaction.createKeyValueCursor();
        cursor.put(42, value);
        assertTrue(Arrays.equals(value, cursor.get(42)));
        cursor.close();
        transaction.abort();
    }

    @Test
    public void testFirstLastNextPrev() {
        Transaction transaction = store.beginTx();
        KeyValueCursor cursor = transaction.createKeyValueCursor();
        cursor.put(1, new byte[]{1, 2, 3, 4});
        cursor.put(2, new byte[]{2, 3, 4, 5});
        cursor.put(4, new byte[]{4, 5, 6, 7});
        cursor.put(8, new byte[]{8, 9, 10, 11, 12, 13, 14, 15});

        assertTrue(Arrays.equals(new byte[]{1, 2, 3, 4}, cursor.getFirst()));
        assertTrue(Arrays.equals(new byte[]{2, 3, 4, 5}, cursor.getNext()));
        assertTrue(Arrays.equals(new byte[]{4, 5, 6, 7}, cursor.getNext()));
        assertTrue(Arrays.equals(new byte[]{2, 3, 4, 5}, cursor.getPrev()));
        // getLast is currently unsupported
        //        assertTrue(Arrays.equals(new byte[]{8, 9, 10, 11, 12, 13}, cursor.getLast()));
        //        assertTrue(Arrays.equals(new byte[]{4, 5, 6, 7, 8}, cursor.getPrev()));

        cursor.close();
        transaction.abort();
    }

    @Test
    public void testRemove() {
        try (Transaction transaction = store.beginTx()) {
            KeyValueCursor cursor = transaction.createKeyValueCursor();

            cursor.put(1, new byte[]{1, 1, 0, 0});
            cursor.put(2, new byte[]{2, 1, 0, 0});
            cursor.put(4, new byte[]{4, 1, 0, 0});

            assertTrue(cursor.removeAt(2));

            // now 4 should be next to 1
            assertTrue(cursor.seek(1));
            byte[] next = cursor.getNext();
            assertNotNull(next);
            assertTrue(Arrays.equals(new byte[]{4, 1, 0, 0}, next));
        }
    }

    @Test
    public void testGetEqualOrGreater() {
        Transaction transaction = store.beginTx();
        KeyValueCursor cursor = transaction.createKeyValueCursor();
        cursor.put(1, new byte[]{1, 1, 0, 0});
        cursor.put(2, new byte[]{2, 1, 0, 0});
        cursor.put(4, new byte[]{4, 1, 0, 0});
        cursor.put(4, new byte[]{4, 2, 0, 0});
        cursor.put(8, new byte[]{8, 1, 0, 0});
        cursor.put(16, new byte[]{16, 1, 0, 0});

        assertTrue(Arrays.equals(new byte[]{4, 2, 0, 0}, cursor.getEqualOrGreater(3)));
        assertTrue(Arrays.equals(new byte[]{4, 2, 0, 0}, cursor.getCurrent()));
        assertTrue(Arrays.equals(new byte[]{8, 1, 0, 0}, cursor.getNext()));
        assertTrue(Arrays.equals(new byte[]{8, 1, 0, 0}, cursor.getCurrent()));
        assertEquals(8, cursor.getKey());

        cursor.getFirst();
        cursor.getEqualOrGreater(3);
        assertEquals(4, cursor.getKey());

        cursor.getFirst();
        cursor.getEqualOrGreater(4);
        assertEquals(4, cursor.getKey());

        assertTrue(Arrays.equals(new byte[]{4, 2, 0, 0}, cursor.getCurrent()));
        assertTrue(Arrays.equals(new byte[]{8, 1, 0, 0}, cursor.getNext()));
        assertTrue(Arrays.equals(new byte[]{8, 1, 0, 0}, cursor.getCurrent()));
        assertEquals(8, cursor.getKey());
/*

        // TODO adding this will cause a crash, missing error handling?????
        byte[] r = cursor.getEqualOrGreater(2000);
        Log.i("OS", "XXX " + r[0] + " " + r[1]);
        assertTrue(Arrays.equals(new byte[]{4, 1}, r));
*/
        cursor.close();
        transaction.abort();
    }

    @Test
    public void testPutAndGetNext() {
        byte[] value = {23, 27, 42, 66};
        byte[] value2 = {0xc, 0xa, 0xf, 0xe};
        Transaction transaction = store.beginTx();
        KeyValueCursor cursor = transaction.createKeyValueCursor();
        cursor.put(23, value);
        assertEquals(23, cursor.getKey());
        cursor.put(42, value2);
        assertEquals(42, cursor.getKey());
        assertTrue(Arrays.equals(value, cursor.get(23)));
        assertEquals(23, cursor.getKey());
        assertTrue(Arrays.equals(value2, cursor.getNext()));
        assertEquals(42, cursor.getKey());
        cursor.close();
        transaction.abort();
    }

    @Test
    public void testPutAndGetStressTest() {
        int count = EXTENSIVE ? 10000 : 250;
        int valueSize = 512;
        Random random = new Random(42);

        long start = time();
        Transaction transaction = store.beginTx();
        KeyValueCursor cursor = transaction.createKeyValueCursor();
        byte[] byteArray = new byte[valueSize];
        for (int key = 1; key <= count; key++) {
            random.nextBytes(byteArray);
            cursor.put(key, byteArray);
            if (key % 100 == 0) {
                cursor.close();
                transaction.commit();
                transaction = store.beginTx();
                cursor = transaction.createKeyValueCursor();
            }
        }
        cursor.close();
        transaction.commit();
        long time = time() - start;
        if (time == 0) time = 1;
        log("Wrote " + count + " values 1-by-1 +random: " + time + "ms, " + (count * 1000 / time) + " values/s");

        random = new Random(42);
        start = time();

        transaction = store.beginTx();
        cursor = transaction.createKeyValueCursor();
        for (int key = 1; key <= count; key++) {
            random.nextBytes(byteArray);
            byte[] byteArrayRead = cursor.get(key);
            assertNotNull(byteArrayRead);
            assertTrue(Arrays.equals(byteArray, byteArrayRead));
        }
        cursor.close();
        transaction.abort();
        time = time() - start;
        if (time == 0) time = 1;
        log("Read " + count + " values 1-by-1 +random: " + time + "ms, " + (count * 1000 / time) + " values/s");
    }

}
