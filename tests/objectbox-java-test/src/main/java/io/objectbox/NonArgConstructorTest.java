package io.objectbox;

import org.junit.Before;
import org.junit.Test;

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
        long key = box.put(entity);

        TestEntity entityRead = box.get(key);
        assertNotNull(entityRead);
        assertTrue(entityRead.noArgsConstructorCalled);
        assertEquals(1977, entityRead.getSimpleInt());
    }

}
