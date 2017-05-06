package io.objectbox.relation;

import org.junit.Test;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
    public void testRemoveOrders() {
        int count = 5;
        Customer customer = putCustomerWithOrders(count);
        ToMany<Order> toMany = (ToMany<Order>) customer.orders;
        assertFalse(toMany.isResolved());

        toMany.clear();
        assertEquals(count, countOrdersWithCustomerId(customer.getId()));
        customerBox.put(customer);
        assertEquals(0, countOrdersWithCustomerId(customer.getId()));
        assertEquals(count, orderBox.count());

        // assertEquals(count, countOrdersWithCustomerId(0));
        // assertEquals(5, orderBox.query().notNull(Order_.customerId).build().count());
        // assertEquals(5, orderBox.query().isNull(Order_.customerId).build().count());
    }

    @Test
    public void testRemoveOrders_removeInDb() {
        int count = 5;
        Customer customer = putCustomerWithOrders(count);
        ToMany<Order> toMany = (ToMany<Order>) customer.orders;
        assertFalse(toMany.isResolved());

        toMany.clear();
        assertEquals(count, countOrdersWithCustomerId(customer.getId()));
        customerBox.put(customer);
        assertEquals(0, orderBox.count());

    }

    private long countOrdersWithCustomerId(long customerId) {
        return orderBox.query().equal(Order_.customerId, customerId).build().count();
    }


    private Customer putCustomerWithOrders(int orderCount) {
        Customer customer = putCustomer();
        for (int i = 1; i <= orderCount; i++) {
            putOrder(customer, "order" + i);
        }
        return customer;
    }
}
