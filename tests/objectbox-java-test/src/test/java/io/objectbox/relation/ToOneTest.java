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

package io.objectbox.relation;

import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

import io.objectbox.Property;
import io.objectbox.TestUtils;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class ToOneTest extends AbstractRelationTest {

    @Test
    public void testTargetId_regularTargetIdProperty() {
        Order entity = putOrder(null, null);
        ToOne<Customer> toOne = new ToOne<>(entity, getRelationInfo(Order_.customerId));
        entity.setCustomerId(1042);
        assertEquals(1042, toOne.getTargetId());

        toOne.setTargetId(1977);
        assertEquals(1977, entity.getCustomerId());
    }

    private RelationInfo<Order, Customer> getRelationInfo(Property targetIdProperty) {
        return new RelationInfo<>(new Order_(), new Customer_(), targetIdProperty, null);
    }

    private RelationInfo<Order, Customer> getRelationInfoVirtualTargetProperty() {
        Property<Order> virtualTargetProperty = new Property<>(Order_.__INSTANCE, 2, 3, long.class, "customerId", true);
        return new RelationInfo<>(new Order_(), new Customer_(), virtualTargetProperty, null);
    }

    @Test
    public void testTargetId_virtualTargetIdProperty() {
        Order entity = putOrder(null, null);
        ToOne<Customer> toOne = new ToOne<>(entity, getRelationInfoVirtualTargetProperty());
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

        // With virtual customerId
        ToOne<Customer> toOne = new ToOne<>(source, getRelationInfoVirtualTargetProperty());
        toOne.setTargetId(1977);
        assertEquals("target1", toOne.getTarget().getName());

        toOne.setTarget(target2);
        assertEquals(target2.getId(), toOne.getTargetId());

        // With regular customerId
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

    @Test
    public void testSerializable() throws IOException, ClassNotFoundException {
        Order source = new Order();
        source.setText("source");
        ToOne<Customer> toOne = new ToOne<>(source, getRelationInfo(Order_.customerId));
        Order entityDeserialized = (Order) TestUtils.serializeDeserialize(toOne).getEntity();
        assertEquals("source", entityDeserialized.getText());

        Customer target = new Customer();
        target.setName("target");
        toOne.setTarget(target);

        Customer targetDeserialized = TestUtils.serializeDeserialize(toOne).getTarget();
        assertEquals("target", targetDeserialized.getName());
    }

}
