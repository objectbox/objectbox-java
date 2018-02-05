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

public class DebugCursorTest extends AbstractObjectBoxTest {

    @Test
    public void testDebugCursor_seekOrNext_get() {
        TestEntity entity = putTestEntity("foobar", 42);

        Transaction transaction = store.beginReadTx();
        DebugCursor debugCursor = DebugCursor.create(transaction);

        ByteBuffer bytes = ByteBuffer.allocate(8);
        int partitionPrefix = (6 << 26) | (1 << 2);
        bytes.putInt(partitionPrefix);
        bytes.putInt(0);
        byte[] entityKey = debugCursor.seekOrNext(bytes.array());
        System.out.println(Arrays.toString(entityKey));
        Assert.assertNotNull(entityKey);
        Assert.assertEquals(8, entityKey.length);
        Assert.assertEquals((byte) entity.getId(), entityKey[entityKey.length - 1]);

        byte[] value = debugCursor.get(entityKey);
        Assert.assertNotNull(value);

        debugCursor.close();
        transaction.abort();
    }

}
