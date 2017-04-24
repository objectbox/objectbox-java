package io.objectbox.relation;

import org.junit.Ignore;
import org.junit.Test;


import io.objectbox.Property;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class ToOneTest extends AbstractRelationTest {

    @Test
    public void testTargetId_withTargetIdProperty() {
        Order entity = putOrder(null, null);
        ToOne<Customer> toOne = new ToOne<>(entity, getRelationInfo(Order_.customerId));
        entity.setCustomerId(1042);
        assertEquals(1042, toOne.getTargetId());

        toOne.setTargetId(1977);
        assertEquals(1977, entity.getCustomerId());
    }

    private RelationInfo<Customer> getRelationInfo(Property targetIdProperty) {
        return new RelationInfo<>(new Order_(), new Customer_(), targetIdProperty);
    }

    @Test
    public void testTargetId_noTargetIdProperty() {
        Order entity = putOrder(null, null);
        ToOne<Customer> toOne = new ToOne<>(entity, getRelationInfo(null));
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
        ToOne<Customer> toOne = new ToOne<>(source, getRelationInfo(null));
        toOne.setTargetId(1977);
        assertEquals("target1", toOne.getTarget().getName());

        toOne.setTarget(target2);
        assertEquals(target2.getId(), toOne.getTargetId());

        // With customerId
        toOne = new ToOne<>(source, getRelationInfo(Order_.customerId));
        source.setCustomerId(1977);
        assertEquals("target1", toOne.getTarget().getName());

        toOne.setTarget(target2);
        assertEquals(target2.getId(), toOne.getTargetId());
    }

    @Test
    public void testPutNewSourceAndTarget() {
        Order source = new Order();
        source.setText("source");
        Customer target = new Customer();
        target.setName("target1");

        ToOne<Customer> toOne = source.customer__toOne;
        assertTrue(toOne.isResolved());
        assertTrue(toOne.isNull());
        assertNull(toOne.getCachedTarget());

        toOne.setTarget(target);
        assertSame(target, toOne.getTarget());

        orderBox.put(source);
        long targetId = target.getId();
        assertTrue(targetId != 0);
        assertEquals(targetId, source.getCustomerId());
        assertSame(target, toOne.getCachedTarget());
        assertSame(target, toOne.getTarget());
        assertSame(targetId, toOne.getTargetId());
        assertTrue(toOne.isResolved());
        assertFalse(toOne.isNull());

        // Check reload
        Order reloaded = orderBox.get(source.getId());
        assertNotSame(source, reloaded);
        assertEquals(targetId, reloaded.getCustomerId());
    }

    @Test
    @Ignore("not yet supported")
    public void testPutSourceAndNewTarget() {
        Order source = new Order();
        source.setText("source");
        orderBox.put(source);

        Customer target = new Customer();
        target.setName("target1");

        ToOne<Customer> toOne = new ToOne<>(source, getRelationInfo(Order_.customerId));
        toOne.setTarget(target);

        assertTrue(target.getId() != 0);
        assertEquals("target1", customerBox.get(target.getId()).getName());
    }
}
