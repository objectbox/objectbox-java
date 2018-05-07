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

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.objectbox.Cursor;
import io.objectbox.InternalAccess;

import static org.junit.Assert.*;

/**
 * Testing "standalone" relations (no to-one property).
 */
public class ToManyStandaloneTest extends AbstractRelationTest {

    @Test
    public void testPutAndGetPrimitives() {
        Order order1 = putOrder(null, "order1");
        Order order2 = putOrder(null, "order2");
        Customer customer = putCustomer();
        long customerId = customer.getId();

        Cursor<Customer> cursorSource = InternalAccess.getWriter(customerBox);
        long[] orderIds = {order1.getId(), order2.getId()};
        cursorSource.modifyRelations(1, customerId, orderIds, false);
        RelationInfo<Order> info = Customer_.ordersStandalone;
        int sourceEntityId = info.sourceInfo.getEntityId();
        Cursor<Order> targetCursor = cursorSource.getTx().createCursor(Order.class);
        List<Order> related = targetCursor.getRelationEntities(sourceEntityId, info.relationId, customerId, false);
        assertEquals(2, related.size());
        assertEquals(order1.getId(), related.get(0).getId());
        assertEquals(order2.getId(), related.get(1).getId());

        // Also
        InternalAccess.commitWriter(customerBox, cursorSource);
        assertEquals(2,
                orderBox.internalGetRelationEntities(sourceEntityId, info.relationId, customerId, false).size());
    }

    @Test
    public void testGet() {
        Customer customer = putCustomerWithOrders(2);
        customer = customerBox.get(customer.getId());
        final ToMany<Order> toMany = customer.getOrdersStandalone();

        //        RelationInfo<Order> info = Customer_.ordersStandalone;
        //        int sourceEntityId = info.sourceInfo.getEntityId();
        //        assertEquals(2, orderBox.internalGetRelationEntities(sourceEntityId, info.relationId, customer.getId()).size());

        assertGetOrder1And2(toMany);
    }

    private void assertGetOrder1And2(ToMany<Order> toMany) {
        assertFalse(toMany.isResolved());
        assertEquals(2, toMany.size());
        assertTrue(toMany.isResolved());

        assertEquals("order1", toMany.get(0).getText());
        assertEquals("order2", toMany.get(1).getText());
    }

    @Test
    public void testGetInTx() {
        Customer customer = putCustomerWithOrders(2);
        customer = customerBox.get(customer.getId());
        final ToMany<Order> toMany = customer.getOrdersStandalone();

        store.runInReadTx(new Runnable() {
            @Override
            public void run() {
                assertGetOrder1And2(toMany);
            }
        });
    }

    @Test
    public void testReset() {
        Customer customer = putCustomerWithOrders(2);
        customer = customerBox.get(customer.getId());
        ToMany<Order> toMany = customer.getOrdersStandalone();
        assertEquals(2, toMany.size());

        Customer customer2 = customerBox.get(customer.getId());
        customer2.getOrdersStandalone().add(putOrder(null, "order3"));
        customer2.getOrdersStandalone().applyChangesToDb();

        assertEquals(2, toMany.size());
        toMany.reset();
        assertFalse(toMany.isResolved());
        assertEquals(3, toMany.size());
        assertTrue(toMany.isResolved());
    }

    @Test
    public void testPutNewCustomerWithNewOrders() {
        Customer customer = new Customer();
        testPutCustomerWithOrders(customer, 5, 0);
    }

    @Test
    public void testPutCustomerWithNewOrders() {
        Customer customer = putCustomer();
        testPutCustomerWithOrders(customer, 5, 0);
    }

    @Test
    public void testPutNewCustomerWithNewAndExistingOrders() {
        Customer customer = new Customer();
        testPutCustomerWithOrders(customer, 5, 5);
    }

    @Test
    public void testPutCustomerWithNewAndExistingOrders() {
        Customer customer = putCustomer();
        testPutCustomerWithOrders(customer, 5, 5);
    }

    private void testPutCustomerWithOrders(Customer customer, int countNewOrders, int countExistingOrders) {
        ToMany<Order> toMany = customer.ordersStandalone;
        for (int i = 1; i <= countNewOrders; i++) {
            Order order = new Order();
            order.setText("new" + i);
            toMany.add(order);
        }
        for (int i = 1; i <= countExistingOrders; i++) {
            toMany.add(putOrder(null, "existing" + i));
        }

        assertEquals(countNewOrders + countExistingOrders, toMany.getAddCount());
        customerBox.put(customer);
        assertEquals(1, customer.getId());
        assertEquals(0, toMany.getAddCount());

        for (int i = 1; i <= countNewOrders; i++) {
            assertEquals(countExistingOrders + i, toMany.get(i - 1).getId());
        }

        assertEquals(1, customerBox.count());
        assertEquals(countNewOrders + countExistingOrders, orderBox.count());

        for (Order order : customer.ordersStandalone) {
            assertEquals(0, order.getCustomerId()); // Standalone relations do not set the ID
        }
    }

    @Test
    public void testAddAll() {
        Customer customer = putCustomer();
        ToMany<Order> toMany = customer.ordersStandalone;

        List<Order> orders = new ArrayList<>();
        Order order1 = new Order();
        order1.setText("order1");
        Order order2 = new Order();
        order2.setText("order2");
        orders.add(order1);
        orders.add(order2);
        toMany.addAll(orders);
        customerBox.put(customer);

        List<Order> all = orderBox.getAll();
        assertEquals(2, all.size());
        assertEquals("order1", all.get(0).getText());
        assertEquals("order2", all.get(1).getText());

        toMany.reset();
        assertEquals(2, toMany.size());
    }

    @Test
    public void testClear() {
        int count = 5;
        Customer customer = putCustomerWithOrders(count);
        ToMany<Order> toMany = customer.ordersStandalone;
        toMany.clear();
        customerBox.put(customer);
        Customer customer2 = customerBox.get(customer.getId());
        assertEquals(0, customer2.getOrdersStandalone().size());
        assertEquals(count, orderBox.count());
    }

    @Test
    public void testClear_removeFromTargetBox() {
        Customer customer = putCustomerWithOrders(5);
        ToMany<Order> toMany = customer.ordersStandalone;
        toMany.setRemoveFromTargetBox(true);
        toMany.clear();
        customerBox.put(customer);
        assertEquals(0, orderBox.count());
    }

    @Test
    public void testRemove() {
        int count = 5;
        Customer customer = putCustomerWithOrders(count);
        ToMany<Order> toMany = customer.ordersStandalone;
        Order removed1 = toMany.remove(3);
        assertEquals("order4", removed1.getText());
        Order removed2 = toMany.get(1);
        assertTrue(toMany.remove(removed2));
        customerBox.put(customer);
        assertOrder2And4Removed(count, customer, toMany);
    }

    @Test
    public void testAddRemove_notPersisted() {
        Customer customer = putCustomer();
        ToMany<Order> toMany = customer.ordersStandalone;
        Order order = new Order();
        toMany.add(order);
        toMany.remove(order);
        customerBox.put(customer);
        assertEquals(0, orderBox.count());
    }

    @Test
    public void testRemoveAll() {
        int count = 5;
        Customer customer = putCustomerWithOrders(count);
        ToMany<Order> toMany = customer.ordersStandalone;
        List<Order> toRemove = new ArrayList<>();
        toRemove.add(toMany.get(1));
        toRemove.add(toMany.get(3));
        assertTrue(toMany.removeAll(toRemove));
        customerBox.put(customer);
        assertOrder2And4Removed(count, customer, toMany);
    }

    @Test
    public void testRetainAll() {
        int count = 5;
        Customer customer = putCustomerWithOrders(count);
        ToMany<Order> toMany = customer.ordersStandalone;
        List<Order> toRetain = new ArrayList<>();
        toRetain.add(toMany.get(0));
        toRetain.add(toMany.get(2));
        toRetain.add(toMany.get(4));
        assertTrue(toMany.retainAll(toRetain));
        customerBox.put(customer);
        assertOrder2And4Removed(count, customer, toMany);
    }

    @Test
    public void testSet() {
        int count = 5;
        Customer customer = putCustomerWithOrders(count);
        ToMany<Order> toMany = customer.ordersStandalone;
        Order order1 = new Order();
        order1.setText("new1");
        assertEquals("order2", toMany.set(1, order1).getText());
        Order order2 = putOrder(null, "new2");
        assertEquals("order4", toMany.set(3, order2).getText());
        assertEquals(count + 1, orderBox.count());
        customerBox.put(customer);
        assertEquals(count + 2, orderBox.count());

        toMany.reset();
        assertEquals(5, toMany.size());
        assertEquals("order1", toMany.get(0).getText());
        assertEquals("order3", toMany.get(1).getText());
        assertEquals("order5", toMany.get(2).getText());
        assertEquals("new2", toMany.get(3).getText());
        assertEquals("new1", toMany.get(4).getText());
    }

    private void assertOrder2And4Removed(int count, Customer customer, ToMany<Order> toMany) {
        assertEquals(count, orderBox.count());

        toMany.reset();
        assertEquals(3, toMany.size());
        assertEquals("order1", toMany.get(0).getText());
        assertEquals("order3", toMany.get(1).getText());
        assertEquals("order5", toMany.get(2).getText());
    }

    @Test
    public void testAddRemoved() {
        int count = 5;
        Customer customer = putCustomerWithOrders(count);
        ToMany<Order> toMany = customer.ordersStandalone;
        Order order = toMany.get(2);
        assertTrue(toMany.remove(order));
        assertTrue(toMany.add(order));
        assertTrue(toMany.remove(order));
        assertTrue(toMany.add(order));
        assertEquals(0, toMany.getRemoveCount());
        assertEquals(1, toMany.getAddCount());
        customerBox.put(customer);
        assertEquals(count, orderBox.count());
    }

    @Test
    public void testSyncToTargetBox() {
        int count = 5;
        Customer customer = putCustomerWithOrders(count);
        ToMany<Order> toMany = customer.ordersStandalone;
        Order order = toMany.get(2);
        assertTrue(toMany.retainAll(Collections.singletonList(order)));

        toMany.add(putOrder(null, "new1"));
        Order order2 = new Order();
        order2.setText("new2");
        toMany.add(order2);

        assertEquals(4, toMany.getRemoveCount());
        assertEquals(2, toMany.getAddCount());
        toMany.applyChangesToDb();
        assertEquals(0, toMany.getRemoveCount());
        assertEquals(0, toMany.getAddCount());

        assertEquals(count + 2, orderBox.count());
        toMany.reset();
        assertEquals(3, toMany.size());
        assertEquals("order3", toMany.get(0).getText());
        assertEquals("new1", toMany.get(1).getText());
        assertEquals("new2", toMany.get(2).getText());

        assertFalse(toMany.internalCheckApplyToDbRequired());
        customerBox.put(customer);
        assertEquals(3, customerBox.get(customer.getId()).ordersStandalone.size());
    }

    @Test
    public void testSortById() {
        Customer customer = putCustomerWithOrders(1);
        ToMany<Order> toMany = customer.ordersStandalone;
        Order orderNew1 = new Order();
        orderNew1.setText("new1");
        toMany.add(orderNew1);
        Order order2 = putOrder(null, "order2");
        Order order3 = putOrder(null, "order3");
        toMany.add(order3);
        Order orderNew2 = new Order();
        orderNew2.setText("new2");
        toMany.add(orderNew2);
        toMany.add(order2);
        toMany.sortById();

        assertEquals("order1", toMany.get(0).getText());
        assertEquals("order2", toMany.get(1).getText());
        assertEquals("order3", toMany.get(2).getText());
        assertEquals("new1", toMany.get(3).getText());
        assertEquals("new2", toMany.get(4).getText());
    }

    private Customer putCustomerWithOrders(int orderCount) {
        Customer customer = new Customer();
        customer.setName("Joe");
        for (int i = 1; i <= orderCount; i++) {
            Order order = new Order();
            order.setText("order" + i);
            customer.getOrdersStandalone().add(order);
        }
        customerBox.put(customer);
        return customer;
    }
}
