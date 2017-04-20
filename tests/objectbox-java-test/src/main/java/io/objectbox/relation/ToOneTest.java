package io.objectbox.relation;

import org.junit.Before;
import org.junit.Test;

import io.objectbox.AbstractObjectBoxTest;
import io.objectbox.Box;
import io.objectbox.TestEntity;
import io.objectbox.TestEntity_;


import static org.junit.Assert.assertEquals;

public class ToOneTest extends AbstractRelationTest {

    @Test
    public void testTargetId_withTargetIdProperty() {
        Order entity = putOrder(null, null);
        ToOne<Customer> toOne = new ToOne<>(entity, Order_.customerId, Customer.class);
        entity.setCustomerId(1042);
        assertEquals(1042, toOne.getTargetId());

        toOne.setTargetId(1977);
        assertEquals(1977, entity.getCustomerId());
    }

    @Test
    public void testTargetId_noTargetIdProperty() {
        Order entity = putOrder(null, null);
        ToOne<Customer> toOne = new ToOne<>(entity, null, Customer.class);
        entity.setCustomerId(1042);
        assertEquals(0, toOne.getTargetId());
        toOne.setTargetId(1977);
        assertEquals(1042, entity.getCustomerId());
        assertEquals(1977, toOne.getTargetId());
    }

    @Test
    public void testGetTarget() {
        Customer target = new Customer();
        target.setName("target1");
        target.setId(1977);
        Customer target2 = new Customer();
        target2.setName("target2");
        target2.setId(2001);
        customerBox.put(target, target2);

        Order source = putOrder(null, null);
        ToOne<Customer> toOne = new ToOne<>(source, null, Customer.class);
        toOne.setTargetId(1977);
        assertEquals("target1", toOne.getTarget().getName());
    }
}
