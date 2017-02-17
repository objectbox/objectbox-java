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

public class ObjectClassObserverTest extends AbstractObjectBoxTest {

    protected BoxStore createBoxStore() {
        return createBoxStoreBuilderWithTwoEntities(false).build();
    }

    CountDownLatch observerLatch = new CountDownLatch(1);

    final List<Class> classesWithChanges = new ArrayList<>();

    DataObserver objectClassObserver = new DataObserver<Class>() {
        @Override
        public void onData(Class objectClass) {
            classesWithChanges.add(objectClass);
            observerLatch.countDown();
        }
    };

    Runnable txRunnable = new Runnable() {
        @Override
        public void run() {
            putTestEntities(3);
            Box<TestEntityMinimal> boxMini = store.boxFor(TestEntityMinimal.class);
            boxMini.put(new TestEntityMinimal(), new TestEntityMinimal());
            assertEquals(0, classesWithChanges.size());
        }
    };

    @Before
    public void clear() {
        classesWithChanges.clear();
    }

    @Test
    public void testTwoObjectClassesChanged_catchAllListener() {
        testTwoObjectClassesChanged_catchAllListener(false);
    }

    @Test
    public void testTwoObjectClassesChanged_catchAllListenerWeak() {
        testTwoObjectClassesChanged_catchAllListener(true);
    }

    public void testTwoObjectClassesChanged_catchAllListener(boolean weak) {
        DataSubscription subscription = subscribe(weak, null);
        store.runInTx(new Runnable() {
            @Override
            public void run() {
                // Dummy TX, still will be committed
                getTestEntityBox().count();
            }
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
        SubscriptionBuilder<Class> subscriptionBuilder = store.subscribe(forClass);
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

        SubscriptionBuilder<Long> subscriptionBuilder = store.subscribe().transform(new DataTransformer<Class, Long>() {
            @Override
            public Long transform(Class source) throws Exception {
                assertNotSame(testThread, Thread.currentThread());
                return store.boxFor(source).count();
            }
        });
        if (scheduler != null) {
            subscriptionBuilder.on(scheduler);
        }
        DataSubscription subscription = subscriptionBuilder.observer(new DataObserver<Long>() {
            @Override
            public void onData(Long data) {
                objectCounts.add(data);
                latch.countDown();
            }
        });

        store.runInTx(new Runnable() {
            @Override
            public void run() {
                // Dummy TX, still will be committed, should not add anything to objectCounts
                getTestEntityBox().count();
            }
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
    public void testScheduler() throws InterruptedException {
        TestScheduler scheduler = new TestScheduler();
        store.subscribe().on(scheduler).observer(objectClassObserver);

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

        DataSubscription subscription = store.subscribe().transform(new DataTransformer<Class, Long>() {
            @Override
            public Long transform(Class source) throws Exception {
                throw new Exception("Boo");
            }
        }).onError(new ErrorObserver() {
            @Override
            public void onError(Throwable th) {
                assertNotSame(testThread, Thread.currentThread());
                errors.add(th);
                latch.countDown();
            }
        }).on(scheduler).observer(new DataObserver<Long>() {
            @Override
            public void onData(Long data) {
                throw new RuntimeException("Should not reach this");
            }
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

        DataSubscription subscription = store.subscribe().onError(new ErrorObserver() {
            @Override
            public void onError(Throwable th) {
                assertNotSame(testThread, Thread.currentThread());
                errors.add(th);
                latch.countDown();
            }
        }).on(scheduler).observer(new DataObserver<Class>() {
            @Override
            public void onData(Class data) {
                throw new RuntimeException("Boo");
            }
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

}