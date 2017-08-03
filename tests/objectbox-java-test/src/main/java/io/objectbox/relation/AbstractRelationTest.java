package io.objectbox.relation;

import org.junit.After;
import org.junit.Before;

import java.io.File;

import io.objectbox.AbstractObjectBoxTest;
import io.objectbox.Box;
import io.objectbox.BoxStore;
import io.objectbox.BoxStoreBuilder;

public abstract class AbstractRelationTest extends AbstractObjectBoxTest {

    protected Box<Order> orderBox;
    protected Box<Customer> customerBox;

    @Override
    protected BoxStore createBoxStore() {
        return MyObjectBox.builder().baseDirectory(boxStoreDir).debugTransactions().build();
    }

    @After
    public void deleteDbFiles() {
        BoxStore.deleteAllFiles(new File(BoxStoreBuilder.DEFAULT_NAME));
    }

    @Before
    public void initBoxes() {
        deleteDbFiles();
        customerBox = store.boxFor(Customer.class);
        orderBox = store.boxFor(Order.class);
        customerBox.removeAll();
        orderBox.removeAll();
    }

    protected Customer putCustomer() {
        Customer customer = new Customer();
        customer.setName("Joe");
        customerBox.put(customer);
        return customer;
    }

    protected Order putOrder(Customer customer, String text) {
        Order order = new Order();
        order.setCustomer(customer);
        order.setText(text);
        orderBox.put(order);
        return order;
    }

}
