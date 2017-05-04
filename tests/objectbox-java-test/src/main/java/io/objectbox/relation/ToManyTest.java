package io.objectbox.relation;

import org.junit.Test;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ToManyTest extends AbstractRelationTest {

    @Test
    public void testGet() {
        Customer customer = putCustomerWithOrders();
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
        Customer customer = putCustomerWithOrders();
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
        Order order1 = new Order();
        order1.setText("foo");
        Order order2 = new Order();
        order2.setText("bar");
        customer.orders.add(order1);
        customer.orders.add(order2);

        ToMany<Order> toMany = (ToMany<Order>) customer.orders;
        assertEquals(2, toMany.getAddCount());
        customerBox.put(customer);
        assertEquals(1, customer.getId());
        assertEquals(1, order1.getId());
        assertEquals(2, order2.getId());
        assertEquals(0, toMany.getAddCount());

        assertEquals(1, customerBox.count());
        assertEquals(2, orderBox.count());

        assertEquals(customer.getId(), order1.getCustomerId());
        assertEquals(customer.getId(), order2.getCustomerId());
        assertEquals(customer.getId(), orderBox.get(order1.getId()).getCustomerId());
        assertEquals(customer.getId(), orderBox.get(order2.getId()).getCustomerId());
    }

    private Customer putCustomerWithOrders() {
        Customer customer = putCustomer();
        putOrder(customer, "order1");
        putOrder(customer, "order2");
        return customer;
    }
}
