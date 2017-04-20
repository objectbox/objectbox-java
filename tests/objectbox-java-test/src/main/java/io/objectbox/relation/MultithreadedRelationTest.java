package io.objectbox.relation;

import org.junit.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

public class MultithreadedRelationTest extends AbstractRelationTest {

    volatile boolean running;

    private CountDownLatch errorLatch = new CountDownLatch(1);
    Throwable error;

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
                        if (all.size() > 100 + random.nextInt(100)) {
                            System.out.println(">>" + all.size());
                            System.out.println(">>>>" + orders.size());
                            orderBox.remove(orders);
                            customerBox.remove(customer);
                        } else if (orders.size() > 1) {
                            orderBox.remove(orders.get(random.nextInt(orders.size())));
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
