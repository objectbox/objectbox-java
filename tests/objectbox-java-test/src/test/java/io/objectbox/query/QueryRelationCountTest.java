package io.objectbox.query;

import io.objectbox.relation.AbstractRelationTest;
import io.objectbox.relation.Customer;
import io.objectbox.relation.Customer_;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class QueryRelationCountTest extends AbstractRelationTest {

    @Test
    public void queryRelationCount() {
        // Customer without orders.
        putCustomer();
        // Customer with 2 orders.
        Customer customerWithOrders = putCustomer();
        putOrder(customerWithOrders, "First order");
        putOrder(customerWithOrders, "Second order");

        // Find customer with no orders.
        try (Query<Customer> query = customerBox
                .query(Customer_.orders.relationCount(0))
                .build()) {
            List<Customer> customer = query.find();
            assertEquals(1, customer.size());
            assertEquals(0, customer.get(0).getOrders().size());
        }

        // Find customer with two orders.
        try (Query<Customer> query = customerBox
                .query(Customer_.orders.relationCount(2))
                .build()) {
            List<Customer> customer = query.find();
            assertEquals(1, customer.size());
            assertEquals(2, customer.get(0).getOrders().size());
        }

        // Find no customer with three orders.
        try (Query<Customer> query = customerBox
                .query(Customer_.orders.relationCount(3))
                .build()) {
            List<Customer> customer = query.find();
            assertEquals(0, customer.size());
        }
    }

}
