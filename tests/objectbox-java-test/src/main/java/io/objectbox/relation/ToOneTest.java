package io.objectbox.relation;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import io.objectbox.AbstractObjectBoxTest;
import io.objectbox.Box;
import io.objectbox.TestEntity;
import io.objectbox.TestEntity_;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
    public void testGetAndSetTarget() {
        Customer target = new Customer();
        target.setName("target1");
        target.setId(1977);
        Customer target2 = new Customer();
        target2.setName("target2");
        target2.setId(2001);
        customerBox.put(target, target2);
        Order source = putOrder(null, null);

        // Without customerId
        ToOne<Customer> toOne = new ToOne<>(source, null, Customer.class);
        toOne.setTargetId(1977);
        assertEquals("target1", toOne.getTarget().getName());

        toOne.setTarget(target2);
        assertEquals(target2.getId(), toOne.getTargetId());

        // With customerId
        toOne = new ToOne<>(source, Order_.customerId, Customer.class);
        source.setCustomerId(1977);
        assertEquals("target1", toOne.getTarget().getName());

        toOne.setTarget(target2);
        assertEquals(target2.getId(), toOne.getTargetId());
    }

    @Test
    @Ignore("not yet supported")
    public void testPutNewSourceAndTarget() {
        Order source = new Order();
        source.setText("source");
        Customer target = new Customer();
        target.setName("target1");

        ToOne<Customer> toOne = new ToOne<>(source, Order_.customerId, Customer.class);
        toOne.setTarget(target);
        orderBox.put(source);

        assertTrue(target.getId() != 0);
    }

    @Test
    @Ignore("not yet supported")
    public void testPutSourceAndNewTarget() {
        Order source = new Order();
        source.setText("source");
        orderBox.put(source);

        Customer target = new Customer();
        target.setName("target1");

        ToOne<Customer> toOne = new ToOne<>(source, Order_.customerId, Customer.class);
        toOne.setTarget(target);

        assertTrue(target.getId() != 0);
        assertEquals("target1", customerBox.get(target.getId()));
    }
}
