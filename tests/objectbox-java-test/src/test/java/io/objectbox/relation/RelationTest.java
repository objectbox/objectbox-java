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

import java.util.Comparator;
import java.util.List;

import io.objectbox.query.Query;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class RelationTest extends AbstractRelationTest {

    @Test
    public void testRelationToOne() {
        Customer customer = putCustomer();
        Order order = putOrder(customer, "Bananas");

        Order order1 = orderBox.get(order.getId());
        assertEquals(customer.getId(), order1.getCustomerId());
        assertNull(order1.peekCustomer());
        assertEquals(customer.getId(), order1.getCustomer().getId());
        assertNotNull(order1.peekCustomer());
    }

    @Test
    public void testRelationToMany() {
        Customer customer = putCustomer();
        Order order1 = putOrder(customer, "Bananas");
        Order order2 = putOrder(customer, "Oranges");

        List<Order> orders = customer.getOrders();
        assertEquals(2, orders.size());
        assertEquals(order1.getId(), orders.get(0).getId());
        assertEquals("Bananas", orders.get(0).getText());
        assertEquals(order2.getId(), orders.get(1).getId());
        assertEquals("Oranges", orders.get(1).getText());
    }

    @Test
    public void testRelationToMany_comparator() {
        Customer customer = putCustomer();
        putOrder(customer, "Bananas");
        putOrder(customer, "Oranges");
        putOrder(customer, "Apples");

        ToMany<Order> orders = (ToMany<Order>) customer.getOrders();
        orders.setComparator(Comparator.comparing(o -> o.text));
        orders.reset();

        assertEquals(3, orders.size());
        assertEquals("Apples", orders.get(0).getText());
        assertEquals("Bananas", orders.get(1).getText());
        assertEquals("Oranges", orders.get(2).getText());
    }

    @Test
    public void testRelationToMany_activeRelationshipChanges() {
        Customer customer = putCustomer();
        Order order1 = putOrder(customer, "Bananas");
        Order order2 = putOrder(customer, "Oranges");

        List<Order> orders = customer.getOrders();
        assertEquals(2, orders.size());
        orderBox.remove(order1);
        ((ToMany<Order>) orders).reset();
        assertEquals(1, orders.size());

        order2.setCustomer(null);
        orderBox.put(order2);

        ((ToMany<Order>) orders).reset();
        assertEquals(0, orders.size());
    }

    @Test
    public void testAttach() {
        Customer customer = putCustomer();
        putOrder(customer, "Bananas");
        putOrder(customer, "Oranges");

        Customer customerNew = new Customer();
        customerNew.setId(customer.getId());
        customerBox.attach(customerNew);
        assertEquals(2, customerNew.getOrders().size());

        customerNew.setName("Jake");
        customerBox.put(customerNew);
        assertEquals("Jake", customerBox.get(customer.getId()).getName());
    }

    @Test
    public void testRelationToOneQuery() {
        Customer customer = putCustomer();
        Order order = putOrder(customer, "Bananas");

        Query<Order> query = orderBox.query().equal(Order_.customerId, customer.getId()).build();
        Order orderFound = query.findUnique();
        assertEquals(order.getId(), orderFound.getId());
    }

    @Test
    public void testToOneBulk() {
        // JNI local refs are limited on Android (for example, 512 on Android 7)
        final int count = runExtensiveTests ? 10000 : 1000;
        store.runInTx(() -> {
            for (int i = 0; i < count; i++) {
                Customer customer = new Customer(0, "Customer" + i);
                customerBox.put(customer);
                putOrder(customer, "order" + 1);
            }
        });
        assertEquals(count, customerBox.getAll().size());
        assertEquals(count, orderBox.getAll().size());
    }

    @Test
    public void testToManyBulk() {
        // JNI local refs are limited on Android (for example, 512 on Android 7)
        final int count = runExtensiveTests ? 10000 : 1000;
        Customer customer = new Customer();
        List<Order> orders = customer.getOrders();
        List<Order> ordersStandalone = customer.getOrdersStandalone();
        for (int i = 0; i < count; i++) {
            Order order = new Order();
            order.setText("order" + i);
            orders.add(order);
            Order orderStandalone = new Order();
            orderStandalone.setText("orderStandalone" + i);
            ordersStandalone.add(orderStandalone);
        }
        long customerId = customerBox.put(customer);

        assertEquals(count, customerBox.get(customerId).getOrders().size());
        assertEquals(count, customerBox.get(customerId).getOrdersStandalone().size());
    }

}
