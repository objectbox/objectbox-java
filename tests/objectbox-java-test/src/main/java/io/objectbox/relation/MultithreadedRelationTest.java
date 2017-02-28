package io.objectbox.relation;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.objectbox.AbstractObjectBoxTest;
import io.objectbox.Box;
import io.objectbox.BoxStore;
import io.objectbox.BoxStoreBuilder;


import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

public class MultithreadedRelationTest extends AbstractObjectBoxTest {

    private Box<Order> orderBox;
    private Box<Customer> customerBox;

    volatile boolean running;

    private CountDownLatch errorLatch = new CountDownLatch(1);
    Throwable error;

    @Override
    protected BoxStore createBoxStore() {
        return MyObjectBox.builder().baseDirectory(boxStoreDir).build();
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

    @Test
    public void testMultithreadedRelations() throws InterruptedException {
        running = true;
        Thread[] threads = new Thread[5];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new MyThread();
            threads[i].start();
        }
        int millis = runExtensiveTests ? 60 * 1000 : 50;
        millis = 60 * 1000;
        boolean hasError = errorLatch.await(millis, TimeUnit.MILLISECONDS);
        running = false;
        assertNull(error);
        assertFalse(hasError);
        for (int i = 0; i < threads.length; i++) {
            threads[i].join();
        }
    }

    private Customer putCustomer() {
        Customer customer = new Customer();
        customer.setName("Joe");
        customerBox.put(customer);
        return customer;
    }

    private Order putOrder(Customer customer, String text) {
        Order order = new Order();
        order.setCustomer(customer);
        order.setText(text);
        orderBox.put(order);
        return order;
    }

    private class MyThread extends Thread {
        @Override
        public void run() {
            try {
                while (running) {
                    Customer customer = null;
                    if (random.nextFloat() > 0.5f) {
                        List<Customer> all = customerBox.getAll();
                        if (!all.isEmpty()) {
                            customer = all.get(random.nextInt(all.size()));
                        }
                    }
                    if (customer == null) {
                        customer = putCustomer();
                    }
                    putOrder(customer, "Bananas");
                    putOrder(customer, "Oranges");
                    putOrder(customer, "Apples");
                    putOrder(customer, "Mangos");

                    final List<Customer> all = customerBox.getAll();
                    if (all.size() > 1) {
                        Customer customer2 = all.get(random.nextInt(all.size()));
                        final List<Order> orders = customer2.getOrders();
                        if (all.size() > 10000 + random.nextInt(500)) {
                            System.out.println(">>" + all.size());
                            System.out.println(">>>>" + orders.size());
                            orderBox.remove(orders);
                            customerBox.remove(customer);
                        }
                    }
                }
            } catch (Throwable th) {
                if (errorLatch.getCount() != 0) {
                    error = th;
                }
                errorLatch.countDown();
            }

        }
    }
}
