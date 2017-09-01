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

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import io.objectbox.ModelBuilder.EntityBuilder;
import io.objectbox.model.EntityFlags;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class NonArgConstructorTest extends AbstractObjectBoxTest {

    private Box<TestEntity> box;

    @Before
    public void setUpBox() {
        box = getTestEntityBox();
    }

    @Override
    protected void addOptionalFlagsToTestEntity(EntityBuilder entityBuilder) {
        entityBuilder.flags(EntityFlags.USE_NO_ARG_CONSTRUCTOR);
    }

    @Test
    public void testPutAndGet() {
        TestEntity entity = new TestEntity();
        entity.setSimpleInt(1977);
        entity.setSimpleBoolean(true);
        entity.setSimpleByte((byte) 42);
        byte[] bytes = {77, 78};
        entity.setSimpleByteArray(bytes);
        entity.setSimpleDouble(3.141);
        entity.setSimpleFloat(3.14f);
        entity.setSimpleLong(789437444354L);
        entity.setSimpleShort((short) 233);
        entity.setSimpleString("foo");
        long key = box.put(entity);

        TestEntity entityRead = box.get(key);
        assertNotNull(entityRead);
        assertTrue(entityRead.noArgsConstructorCalled);
        assertEquals(1977, entityRead.getSimpleInt());
        assertTrue(entityRead.getSimpleBoolean());
        assertEquals(42, entityRead.getSimpleByte());
        assertEquals(233, entityRead.getSimpleShort());
        assertEquals(789437444354L, entityRead.getSimpleLong());
        assertEquals(3.14f, entityRead.getSimpleFloat(), 0.000001f);
        assertEquals(3.141f, entityRead.getSimpleDouble(), 0.000001);
        assertTrue(Arrays.equals(bytes, entityRead.getSimpleByteArray()));
        assertEquals("foo", entityRead.getSimpleString());
    }

}
