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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import io.objectbox.TestUtils;
import io.objectbox.query.QueryFilter;

import static org.junit.Assert.*;

public class ToManyTest extends AbstractRelationTest {

    @Test
    public void testGet() {
        Customer customer = putCustomerWithOrders(2);
        customer = customerBox.get(customer.getId());
        ToMany<Order> toMany = (ToMany<Order>) customer.getOrders();
        assertFalse(toMany.isResolved());
        assertEquals(2, toMany.size());
        assertTrue(toMany.isResolved());

        assertEquals("order1", toMany.get(0).getText());
        assertEquals("order2", toMany.get(1).getText());
    }

    @Test
    public void testReset() {
        Customer customer = putCustomerWithOrders(2);
        customer = customerBox.get(customer.getId());
        ToMany<Order> toMany = (ToMany<Order>) customer.getOrders();
        assertEquals(2, toMany.size());
        putOrder(customer, "order3");
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
        for (int i = 1; i <= countNewOrders; i++) {
            Order order = new Order();
            order.setText("new" + i);
            customer.orders.add(order);
        }
        for (int i = 1; i <= countExistingOrders; i++) {
            customer.orders.add(putOrder(null, "existing" + i));
        }

        ToMany<Order> toMany = (ToMany<Order>) customer.orders;
        assertEquals(countNewOrders + countExistingOrders, toMany.getAddCount());
        customerBox.put(customer);
        assertEquals(1, customer.getId());
        assertEquals(0, toMany.getAddCount());

        for (int i = 1; i <= countNewOrders; i++) {
            assertEquals(countExistingOrders + i, customer.orders.get(i - 1).getId());
        }

        assertEquals(1, customerBox.count());
        assertEquals(countNewOrders + countExistingOrders, orderBox.count());

        for (Order order : customer.orders) {
            assertEquals(customer.getId(), order.getCustomerId());
            assertEquals(customer.getId(), orderBox.get(order.getId()).getCustomerId());
        }
    }

    @Test
    public void testAddAll() {
        Customer customer = putCustomer();
        ToMany<Order> toMany = (ToMany<Order>) customer.orders;

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
        assertEquals(customer.getId(), all.get(0).getCustomerId());
        assertEquals("order2", all.get(1).getText());
        assertEquals(customer.getId(), all.get(1).getCustomerId());

        toMany.reset();
        assertEquals(2, toMany.size());
    }

    @Test
    public void testClear() {
        int count = 5;
        Customer customer = putCustomerWithOrders(count);
        ToMany<Order> toMany = (ToMany<Order>) customer.orders;
        assertFalse(toMany.isResolved());

        toMany.clear();
        assertEquals(count, countOrdersWithCustomerId(customer.getId()));
        customerBox.put(customer);
        assertEquals(0, countOrdersWithCustomerId(customer.getId()));
        assertEquals(count, orderBox.count());
        assertEquals(count, countOrdersWithCustomerId(0));
    }

    @Test
    public void testClear_removeFromTargetBox() {
        Customer customer = putCustomerWithOrders(5);
        ToMany<Order> toMany = (ToMany<Order>) customer.orders;
        toMany.setRemoveFromTargetBox(true);
        toMany.clear();
        customerBox.put(customer);
        assertEquals(0, orderBox.count());
    }

    @Test
    public void testRemove() {
        int count = 5;
        Customer customer = putCustomerWithOrders(count);
        ToMany<Order> toMany = (ToMany<Order>) customer.orders;
        Order removed1 = toMany.remove(3);
        assertEquals("order4", removed1.getText());
        Order removed2 = toMany.get(1);
        assertTrue(toMany.remove(removed2));
        customerBox.put(customer);
        assertOrder2And4Removed(count, customer, toMany);
    }

    @Test
    public void testRemoveAll() {
        int count = 5;
        Customer customer = putCustomerWithOrders(count);
        ToMany<Order> toMany = (ToMany<Order>) customer.orders;
        List<Order> toRemove = new ArrayList<>();
        toRemove.add(toMany.get(1));
        toRemove.add(toMany.get(3));
        assertTrue(toMany.removeAll(toRemove));
        customerBox.put(customer);
        assertOrder2And4Removed(count, customer, toMany);
    }

    @Test
    public void testRemoveById() {
        int count = 5;
        Customer customer = putCustomerWithOrders(count);
        ToMany<Order> toMany = (ToMany<Order>) customer.orders;
        Order removed1 = toMany.removeById(toMany.get(3).getId());
        assertEquals("order4", removed1.getText());
        Order removed2 = toMany.removeById(toMany.get(1).getId());
        assertEquals("order2", removed2.getText());
        assertNull(toMany.removeById(42));
        customerBox.put(customer);
        assertOrder2And4Removed(count, customer, toMany);
    }

    @Test
    public void testRetainAll() {
        int count = 5;
        Customer customer = putCustomerWithOrders(count);
        ToMany<Order> toMany = (ToMany<Order>) customer.orders;
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
        ToMany<Order> toMany = (ToMany<Order>) customer.orders;
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
        assertEquals(count - 2, countOrdersWithCustomerId(customer.getId()));
        assertEquals(count, orderBox.count());
        assertEquals(2, countOrdersWithCustomerId(0));

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
        ToMany<Order> toMany = (ToMany<Order>) customer.orders;
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
    public void testAddRemove() {
        Customer customer = putCustomer();
        ToMany<Order> toMany = (ToMany<Order>) customer.orders;
        Order order = new Order();
        toMany.add(order);
        toMany.remove(order);

        toMany.applyChangesToDb();
        assertEquals(0, orderBox.count());
    }

    @Test
    public void testAddAddRemove() {
        Customer customer = putCustomer();
        ToMany<Order> toMany = (ToMany<Order>) customer.orders;
        assertFalse(toMany.hasPendingDbChanges());
        Order order = new Order();
        toMany.add(order);
        assertTrue(toMany.hasPendingDbChanges());
        toMany.add(order);
        toMany.remove(order);
        assertTrue(toMany.hasPendingDbChanges());
        assertEquals(1, toMany.getAddCount());
        assertEquals(0, toMany.getRemoveCount());

        toMany.applyChangesToDb();
        assertEquals(1, orderBox.count());
    }

    @Test
    public void testReverse() {
        int count = 5;
        Customer customer = putCustomerWithOrders(count);
        ToMany<Order> toMany = (ToMany<Order>) customer.orders;
        Collections.reverse(toMany);

        toMany.applyChangesToDb();
        assertEquals(count, toMany.size());
        toMany.reset();
        assertEquals(count, toMany.size());
    }

    @Test
    public void testSet_Swap() {
        Customer customer = putCustomer();
        ToMany<Order> toMany = (ToMany<Order>) customer.orders;
        assertFalse(toMany.hasPendingDbChanges());
        toMany.add(new Order());
        toMany.add(new Order());
        toMany.add(new Order());

        // Swap 0 and 2 using get and set - this causes 2 to be in the list twice temporarily
        Order order0 = toMany.get(0);
        toMany.set(0, toMany.get(2));
        toMany.set(2, order0);

        assertEquals(3, toMany.getAddCount());
        assertEquals(0, toMany.getRemoveCount());

        toMany.applyChangesToDb();
        assertEquals(3, orderBox.count());
    }

    @Test(expected = IllegalStateException.class)
    public void testSyncToTargetBox_detached() {
        Customer customer = new Customer();
        customer.setId(42);
        ((ToMany<Order>) customer.orders).applyChangesToDb();
    }

    @Test
    public void testSyncToTargetBox() {
        int count = 5;
        Customer customer = putCustomerWithOrders(count);
        ToMany<Order> toMany = (ToMany<Order>) customer.orders;
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
        assertEquals(3, customerBox.get(customer.getId()).orders.size());
    }

    @Test
    public void testSortById() {
        Customer customer = putCustomerWithOrders(1);
        ToMany<Order> toMany = (ToMany<Order>) customer.orders;
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

    @Test
    public void testHasA() {
        Customer customer = putCustomerWithOrders(3);
        ToMany<Order> toMany = (ToMany<Order>) customer.orders;
        QueryFilter<Order> filter = entity -> "order2".equals(entity.text);
        assertTrue(toMany.hasA(filter));
        toMany.remove(1);
        assertFalse(toMany.hasA(filter));
    }

    @Test
    public void testHasAll() {
        Customer customer = putCustomerWithOrders(3);
        ToMany<Order> toMany = (ToMany<Order>) customer.orders;
        QueryFilter<Order> filter = entity -> entity.text.startsWith("order");
        assertTrue(toMany.hasAll(filter));
        toMany.get(0).text = "nope";
        assertFalse(toMany.hasAll(filter));
        toMany.clear();
        assertFalse(toMany.hasAll(filter));
    }

    @Test
    public void testIndexOfId() {
        Customer customer = putCustomerWithOrders(3);
        ToMany<Order> toMany = (ToMany<Order>) customer.orders;
        assertEquals(1, toMany.indexOfId(toMany.get(1).getId()));
        assertEquals(2, toMany.indexOfId(toMany.get(2).getId()));
        assertEquals(0, toMany.indexOfId(toMany.get(0).getId()));
        assertEquals(-1, toMany.indexOfId(42));
    }

    @Test
    public void testGetById() {
        Customer customer = putCustomerWithOrders(3);
        ToMany<Order> toMany = (ToMany<Order>) customer.orders;
        assertEquals(toMany.get(1), toMany.getById(toMany.get(1).getId()));
        assertEquals(toMany.get(2), toMany.getById(toMany.get(2).getId()));
        assertEquals(toMany.get(0), toMany.getById(toMany.get(0).getId()));
        assertNull(toMany.getById(42));
    }

    @Test
    public void testSerializable() throws IOException, ClassNotFoundException {
        Customer customer = new Customer();
        customer.setName("source");
        ToMany<Order> toMany = (ToMany<Order>) customer.orders;

        Customer entityDeserialized = (Customer) TestUtils.serializeDeserialize(toMany).getEntity();
        assertEquals("source", entityDeserialized.getName());

        Order target = new Order();
        target.setText("target");
        toMany.add(target);

        ToMany<Order> toManyDeserialized = TestUtils.serializeDeserialize(toMany);
        assertEquals(1, toManyDeserialized.size());
        Order order = toManyDeserialized.get(0);
        assertEquals("target", order.getText());

        try {
            customerBox.put((Customer) toManyDeserialized.getEntity());
        } catch (IllegalStateException e) {
            // TODO "The ToOne property for Order.customerId is null" -> ToOne is transient; investigate more?
            e.printStackTrace();
        }
    }

    private long countOrdersWithCustomerId(long customerId) {
        return orderBox.query().equal(Order_.customerId, customerId).build().count();
    }

    private Customer putCustomerWithOrders(final int orderCount) {
        return store.callInTxNoException(() -> {
            Customer customer = putCustomer();
            for (int i = 1; i <= orderCount; i++) {
                putOrder(customer, "order" + i);
            }
            return customer;
        });
    }
}
