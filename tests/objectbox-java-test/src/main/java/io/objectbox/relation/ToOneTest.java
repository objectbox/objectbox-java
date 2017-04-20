package io.objectbox.relation;

import org.junit.Before;
import org.junit.Test;

import io.objectbox.AbstractObjectBoxTest;
import io.objectbox.Box;
import io.objectbox.TestEntity;
import io.objectbox.TestEntity_;


import static org.junit.Assert.assertEquals;

public class ToOneTest extends AbstractObjectBoxTest {

    private Box<TestEntity> box;

    @Before
    public void setUpBox() {
        box = getTestEntityBox();
    }

    @Test
    public void testTargetId_withTargetIdProperty() {
        TestEntity entity = putTestEntity("hola", 42);
        ToOne<TestEntity> toOne = new ToOne<>(entity, TestEntity_.simpleLong, TestEntity.class);
        assertEquals(1042, toOne.getTargetId());

        toOne.setTargetId(1977);
        assertEquals(1977, entity.getSimpleLong());
    }

    @Test
    public void testTargetId_noTargetIdProperty() {
        TestEntity entity = putTestEntity("hola", 42);
        ToOne<TestEntity> toOne = new ToOne<>(entity, null, TestEntity.class);
        assertEquals(0, toOne.getTargetId());

        toOne.setTargetId(1977);
        assertEquals(1042, entity.getSimpleLong());
        assertEquals(1977, toOne.getTargetId());
    }
}
