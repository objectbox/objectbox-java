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
