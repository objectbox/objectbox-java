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

package io.objectbox;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import io.objectbox.reactive.DataObserver;
import io.objectbox.reactive.DataSubscription;
import io.objectbox.reactive.DataTransformer;
import io.objectbox.reactive.ErrorObserver;
import io.objectbox.reactive.RunWithParam;
import io.objectbox.reactive.Scheduler;
import io.objectbox.reactive.SubscriptionBuilder;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

@SuppressWarnings({"rawtypes", "unchecked"})
public class ObjectClassObserverTest extends AbstractObjectBoxTest {

    protected BoxStore createBoxStore() {
        return createBoxStoreBuilderWithTwoEntities(false).build();
    }

    CountDownLatch observerLatch = new CountDownLatch(1);

    final List<Class> classesWithChanges = new ArrayList<>();

    DataObserver objectClassObserver = (DataObserver<Class>) objectClass -> {
        classesWithChanges.add(objectClass);
        observerLatch.countDown();
    };

    Runnable txRunnable = () -> {
        putTestEntities(3);
        Box<TestEntityMinimal> boxMini = store.boxFor(TestEntityMinimal.class);
        boxMini.put(new TestEntityMinimal(), new TestEntityMinimal());
        assertEquals(0, classesWithChanges.size());
    };

    @Before
    public void clear() {
        classesWithChanges.clear();
    }

    @Test
    public void testTwoObjectClassesChanged_catchAllObserver() {
        testTwoObjectClassesChanged_catchAllObserver(false);
    }

    @Test
    public void testTwoObjectClassesChanged_catchAllObserverWeak() {
        testTwoObjectClassesChanged_catchAllObserver(true);
    }

    public void testTwoObjectClassesChanged_catchAllObserver(boolean weak) {
        DataSubscription subscription = subscribe(weak, null);
        store.runInTx(() -> {
            // Dummy TX, still will be committed
            getTestEntityBox().count();
        });
        assertEquals(0, classesWithChanges.size());

        runTxAndWaitForObservers(2);

        assertEquals(2, classesWithChanges.size());
        assertTrue(classesWithChanges.contains(TestEntity.class));
        assertTrue(classesWithChanges.contains(TestEntityMinimal.class));

        classesWithChanges.clear();
        subscription.cancel();
        store.runInTx(txRunnable);
        assertNoStaleObservers();
    }

    private DataSubscription subscribe(boolean weak, Class forClass) {
        SubscriptionBuilder<Class> subscriptionBuilder = store.subscribe(forClass).onlyChanges();
        return (weak ? subscriptionBuilder.weak() : subscriptionBuilder).observer(objectClassObserver);
    }

    private void runTxAndWaitForObservers(int latchCount) {
        observerLatch = new CountDownLatch(latchCount);
        store.runInTx(txRunnable);
        assertLatchCountedDown(observerLatch, 5);
    }

    private void assertNoStaleObservers() {
        try {
            Thread.sleep(20);  // Additional time for any stale observers to be notified (not expected)
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        assertEquals(0, classesWithChanges.size());
    }

    @Test
    public void testTwoObjectClassesChanged_oneClassObserver() throws InterruptedException {
        testTwoObjectClassesChanged_oneClassObserver(false);
    }

    @Test
    public void testTwoObjectClassesChanged_oneClassObserverWeak() throws InterruptedException {
        testTwoObjectClassesChanged_oneClassObserver(true);
    }

    public void testTwoObjectClassesChanged_oneClassObserver(boolean weak) throws InterruptedException {
        DataSubscription subscription = subscribe(weak, TestEntityMinimal.class);
        runTxAndWaitForObservers(1);

        assertEquals(1, classesWithChanges.size());
        assertEquals(classesWithChanges.get(0), TestEntityMinimal.class);

        classesWithChanges.clear();
        putTestEntities(1);
        assertEquals(0, classesWithChanges.size());

        // Adding twice should not trigger notification twice
        DataSubscription subscription2 = subscribe(weak, TestEntityMinimal.class);

        Box<TestEntityMinimal> boxMini = store.boxFor(TestEntityMinimal.class);
        observerLatch = new CountDownLatch(1);
        boxMini.put(new TestEntityMinimal(), new TestEntityMinimal());
        assertLatchCountedDown(observerLatch, 5);
        Thread.sleep(20);
        assertEquals(1, classesWithChanges.size());

        classesWithChanges.clear();
        subscription.cancel();
        store.runInTx(txRunnable);
        assertNoStaleObservers();
    }

    @Test
    public void testTransform() throws InterruptedException {
        testTransform(null);
    }

    private void testTransform(TestScheduler scheduler) throws InterruptedException {
        final List<Long> objectCounts = new CopyOnWriteArrayList<>();
        final CountDownLatch latch = new CountDownLatch(2);
        final Thread testThread = Thread.currentThread();

        SubscriptionBuilder<Long> subscriptionBuilder = store.subscribe().onlyChanges().
                transform(source -> {
                    assertNotSame(testThread, Thread.currentThread());
                    return store.boxFor(source).count();
                });
        if (scheduler != null) {
            subscriptionBuilder.on(scheduler);
        }
        DataSubscription subscription = subscriptionBuilder.observer(data -> {
            objectCounts.add(data);
            latch.countDown();
        });

        store.runInTx(() -> {
            // Dummy TX, still will be committed, should not add anything to objectCounts
            getTestEntityBox().count();
        });

        store.runInTx(txRunnable);

        assertLatchCountedDown(latch, 5);
        assertEquals(2, objectCounts.size());
        assertTrue(objectCounts.contains(2L));
        assertTrue(objectCounts.contains(3L));

        objectCounts.clear();
        subscription.cancel();
        store.runInTx(txRunnable);
        Thread.sleep(20);
        assertEquals(0, objectCounts.size());
    }

    @Test
    public void testScheduler() {
        TestScheduler scheduler = new TestScheduler();
        store.subscribe().onlyChanges().on(scheduler).observer(objectClassObserver);

        runTxAndWaitForObservers(2);

        assertEquals(2, scheduler.counter());
        assertEquals(2, classesWithChanges.size());
    }

    @Test
    public void testTransformerWithScheduler() throws InterruptedException {
        TestScheduler scheduler = new TestScheduler();
        testTransform(scheduler);
        assertEquals(2, scheduler.counter());
    }

    private static class TestScheduler implements Scheduler {
        AtomicInteger counter = new AtomicInteger();

        int counter() {
            return counter.intValue();
        }

        @Override
        public <T> void run(RunWithParam runnable, T param) {
            counter.incrementAndGet();
            runnable.run(param);
        }
    }

    @Test
    public void testTransformError() throws InterruptedException {
        testTransformError(null);
    }

    @Test
    public void testTransformErrorWithScheduler() throws InterruptedException {
        TestScheduler scheduler = new TestScheduler();
        testTransformError(scheduler);
        assertEquals(2, scheduler.counter());
    }

    public void testTransformError(Scheduler scheduler) throws InterruptedException {
        final List<Throwable> errors = new CopyOnWriteArrayList<>();
        final CountDownLatch latch = new CountDownLatch(2);
        final Thread testThread = Thread.currentThread();

        DataSubscription subscription = store.subscribe().onlyChanges().transform((DataTransformer<Class, Long>) source -> {
            throw new Exception("Boo");
        }).onError(throwable -> {
            assertNotSame(testThread, Thread.currentThread());
            errors.add(throwable);
            latch.countDown();
        }).on(scheduler).observer(data -> {
            throw new RuntimeException("Should not reach this");
        });

        store.runInTx(txRunnable);

        assertLatchCountedDown(latch, 5);
        assertEquals(2, errors.size());
        assertEquals("Boo", errors.get(0).getMessage());

        errors.clear();
        subscription.cancel();
        store.runInTx(txRunnable);
        Thread.sleep(20);
        assertEquals(0, errors.size());
    }

    @Test
    public void testObserverError() throws InterruptedException {
        testObserverError(null);
    }

    @Test
    public void testObserverErrorWithScheduler() throws InterruptedException {
        TestScheduler scheduler = new TestScheduler();
        testObserverError(scheduler);
        assertEquals(2 + 2, scheduler.counter()); // 2 observer + 2 error observer calls
    }

    public void testObserverError(Scheduler scheduler) throws InterruptedException {
        final List<Throwable> errors = new CopyOnWriteArrayList<>();
        final CountDownLatch latch = new CountDownLatch(2);
        final Thread testThread = Thread.currentThread();

        DataSubscription subscription = store.subscribe().onlyChanges().onError(th -> {
            assertNotSame(testThread, Thread.currentThread());
            errors.add(th);
            latch.countDown();
        }).on(scheduler).observer(data -> {
            throw new RuntimeException("Boo");
        });

        store.runInTx(txRunnable);

        assertLatchCountedDown(latch, 5);
        assertEquals(2, errors.size());
        assertEquals("Boo", errors.get(0).getMessage());

        errors.clear();
        subscription.cancel();
        store.runInTx(txRunnable);
        Thread.sleep(20);
        assertEquals(0, errors.size());
    }

    @Test
    public void testForObserverLeaks() {
        testForObserverLeaks(false, false);
    }

    @Test
    public void testForObserverLeaks_weak() {
        testForObserverLeaks(false, true);
    }

    @Test
    public void testForObserverLeaks_wrapped() {
        testForObserverLeaks(true, false);
    }

    @Test
    public void testForObserverLeaks_wrappedWeak() {
        testForObserverLeaks(true, true);
    }

    public void testForObserverLeaks(boolean wrapped, boolean weak) {
        // Allocation would sum up to 70 GB in total when observer is not unsubscribed
        long maxMB = Math.min(Runtime.getRuntime().maxMemory() / (1024 * 1024), 70L * 1024);
        final int chunkSizeMB = 16; // 16 is faster than 64 & 128 (~0,3s instead of ~1s) and a bit faster than 8 and 32
        int runs = (int) (maxMB / chunkSizeMB + 1);
        for (int i = 0; i < runs; i++) {
            // Use a Scheduler to ensure wrapped observer is used
            SubscriptionBuilder<Class> subscriptionBuilder = store.subscribe().onlyChanges();
            if (weak) {
                subscriptionBuilder.weak();
            }
            if (wrapped) {
                subscriptionBuilder.on(new TestScheduler());
            }
            DataSubscription subscription = subscriptionBuilder.observer(new DataObserver<Class>() {
                byte[] bigMemory = new byte[chunkSizeMB * 1024 * 1024];

                @Override
                public void onData(Class data) {
                    bigMemory[0] ^= 1;
                }
            });
            if (!weak) {
                subscription.cancel();
            }
        }
    }

    @Test
    public void testSingle() {
        testSingle(false, false);
    }

    @Test
    public void testSingle_wrapped() {
        testSingle(false, true);
    }

    @Test
    public void testSingle_weak() {
        testSingle(true, false);
    }

    @Test
    public void testSingle_weakWrapped() {
        testSingle(true, true);
    }

    public void testSingle(boolean weak, boolean wrapped) {
        SubscriptionBuilder<Class> subscriptionBuilder = store.subscribe().single();
        if (weak) {
            subscriptionBuilder.weak();
        }
        if (wrapped) {
            subscriptionBuilder.on(new TestScheduler());
        }
        observerLatch = new CountDownLatch(2);
        subscriptionBuilder.observer(objectClassObserver);
        assertLatchCountedDown(observerLatch, 5);
        assertEquals(2, classesWithChanges.size());
        assertTrue(classesWithChanges.contains(TestEntity.class));
        assertTrue(classesWithChanges.contains(TestEntityMinimal.class));

        classesWithChanges.clear();
        store.runInTx(txRunnable);
        assertNoStaleObservers();
    }

    @Test
    public void testSingleCancelSubscription() throws InterruptedException {
        DataSubscription subscription = store.subscribe().single()
                .transform(source -> {
                    Thread.sleep(20);
                    return source;
                }).observer(objectClassObserver);
        subscription.cancel();
        Thread.sleep(40);
        assertNoStaleObservers();
    }

}