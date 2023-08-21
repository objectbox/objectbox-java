package io.objectbox.relation;

import io.objectbox.query.Query;
import io.objectbox.query.QueryBuilder;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Tests link conditions for queries to filter on related entities.
 * <p>
 * There are more extensive tests in integration tests.
 */
public class LinkQueryTest extends AbstractRelationTest {

    @Test
    public void link_withRegularCondition() {
        Customer john = putCustomer("John");
        putOrder(john, "Apples");
        putOrder(john, "Oranges");

        Customer alice = putCustomer("Alice");
        putOrder(alice, "Apples");
        putOrder(alice, "Bananas");

        // link condition matches orders from Alice
        // simple regular condition matches single order for both
        QueryBuilder<Order> builder = orderBox
                .query(Order_.text.equal("Apples"));
        builder.link(Order_.customer)
                .apply(Customer_.name.equal("Alice").alias("name"));

        try (Query<Order> query = builder.build()) {
            Order order = query.findUnique();
            assertNotNull(order);
            assertEquals("Apples", order.getText());
            assertEquals("Alice", order.getCustomer().getTarget().getName());
        }

        // link condition matches orders from Alice
        // complex regular conditions matches two orders for John, one for Alice
        QueryBuilder<Order> builderComplex = orderBox
                .query(Order_.text.equal("Apples").or(Order_.text.equal("Oranges")));
        builderComplex.link(Order_.customer)
                .apply(Customer_.name.equal("Alice"));

        try (Query<Order> query = builderComplex.build()) {
            Order order = query.findUnique();
            assertNotNull(order);
            assertEquals("Apples", order.getText());
            assertEquals("Alice", order.getCustomer().getTarget().getName());
        }
    }

}
