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

import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.Arrays;

import io.objectbox.internal.DebugCursor;

import static org.junit.Assert.assertEquals;

public class DebugCursorTest extends AbstractObjectBoxTest {

    @Test
    public void testDebugCursor_seekOrNext_get() {
        TestEntity entity1 = putTestEntity("foo", 23);
        TestEntity entity2 = putTestEntity("bar", 42);

        runTest(entity1, entity2);
    }

    @Test
    public void testDebugCursor_seekOrNext_get_withoutModel() {
        TestEntity entity1 = putTestEntity("foo", 23);
        TestEntity entity2 = putTestEntity("bar", 42);

        store.close();
        store = BoxStoreBuilder.createDebugWithoutModel().directory(boxStoreDir).build();

        runTest(entity1, entity2);
    }

    private void runTest(TestEntity entity1, TestEntity entity2) {
        Transaction transaction = store.beginReadTx();
        DebugCursor debugCursor = DebugCursor.create(transaction);

        // seek to first entity
        ByteBuffer bytes = ByteBuffer.allocate(8);
        int partitionPrefix = (6 << 26) | (1 << 2);
        bytes.putInt(partitionPrefix).putInt(0);
        byte[] entity1Key = debugCursor.seekOrNext(bytes.array());
        System.out.println(Arrays.toString(entity1Key));
        Assert.assertNotNull(entity1Key);
        assertEquals(8, entity1Key.length);

        // check key 1
        bytes.rewind();
        bytes.put(entity1Key);
        bytes.flip();
        assertEquals(partitionPrefix, bytes.getInt());
        assertEquals((int) entity1.getId(), bytes.getInt());

        // get value 1
        byte[] value1 = debugCursor.get(entity1Key);
        Assert.assertNotNull(value1);
        Assert.assertTrue(value1.length > 40);

        // get value 2
        bytes.rewind();
        bytes.putInt(partitionPrefix).putInt((int) entity2.getId());
        byte[] value2 = debugCursor.get(bytes.array());
        Assert.assertNotNull(value2);
        Assert.assertTrue(value2.length > 40);

        debugCursor.close();
        transaction.abort();
    }

}
