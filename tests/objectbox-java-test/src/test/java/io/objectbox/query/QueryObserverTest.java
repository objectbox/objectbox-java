/*
 * Copyright 2017 ObjectBox Ltd.
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

package io.objectbox.query;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.objectbox.AbstractObjectBoxTest;
import io.objectbox.Box;
import io.objectbox.ObjectClassObserverTest;
import io.objectbox.TestEntity;
import io.objectbox.reactive.DataObserver;
import io.objectbox.reactive.DataSubscription;


import static io.objectbox.TestEntity_.simpleInt;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class QueryObserverTest extends AbstractObjectBoxTest {

    private Box<TestEntity> box;

    @Before
    public void setUpBox() {
        box = getTestEntityBox();
    }

    @Test
    public void observer_removeDuringCallback_works() throws InterruptedException {
        SelfRemovingObserver testObserver = new SelfRemovingObserver();
        // Note: use onlyChanges to not trigger observer on subscribing.
        testObserver.dataSubscription = box.query().build()
                .subscribe()
                .onlyChanges()
                .observer(testObserver);

        // Trigger event.
        putTestEntitiesScalars();

        // Should have gotten past dataSubscription.cancel() without crashing.
        assertTrue(testObserver.latch.await(5, TimeUnit.SECONDS));

        // Just to make sure: trigger another event, should not be received.
        testObserver.latch = new CountDownLatch(1);
        putTestEntitiesScalars();
        assertFalse(testObserver.latch.await(5, TimeUnit.SECONDS));
    }

    private static class SelfRemovingObserver implements DataObserver<List<TestEntity>> {

        CountDownLatch latch = new CountDownLatch(1);
        DataSubscription dataSubscription;

        @Override
        public void onData(List<TestEntity> data) {
            if (dataSubscription != null) {
                System.out.println("Cancelling subscription");
                dataSubscription.cancel();
                dataSubscription = null;
            }
            // Once here, cancel did not crash.
            latch.countDown();
        }
    }

    @Test
    public void testObserver() {
        int[] valuesInt = {2003, 2007, 2002};
        Query<TestEntity> query = box.query().in(simpleInt, valuesInt).build();
        assertEquals(0, query.count());

        // Initial data on subscription.
        TestObserver<List<TestEntity>> testObserver = new TestObserver<>();
        query.subscribe().observer(testObserver);
        testObserver.assertLatchCountedDown();
        assertEquals(1, testObserver.receivedChanges.size());
        assertEquals(0, testObserver.receivedChanges.get(0).size());

        // On put.
        testObserver.receivedChanges.clear();
        testObserver.resetLatch();
        putTestEntitiesScalars();
        testObserver.assertLatchCountedDown();
        assertEquals(1, testObserver.receivedChanges.size());
        assertEquals(3, testObserver.receivedChanges.get(0).size());

        // On remove all.
        testObserver.receivedChanges.clear();
        testObserver.resetLatch();
        box.removeAll();
        testObserver.assertLatchCountedDown();
        assertEquals(1, testObserver.receivedChanges.size());
        assertEquals(0, testObserver.receivedChanges.get(0).size());
    }

    @Test
    public void observer_resultsDeliveredInOrder() {
        Query<TestEntity> query = box.query().build();

        final CountDownLatch latch = new CountDownLatch(2);
        final AtomicBoolean isLongTransform = new AtomicBoolean(true);
        final List<Integer> placing = new CopyOnWriteArrayList<>();

        // Block first onData call long enough so second one can race it.
        DataSubscription subscription = query.subscribe().observer(data -> {
            if (isLongTransform.compareAndSet(true, false)) {
                // Wait long enough so publish triggered by transaction
                // can overtake publish triggered during observer() call.
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                placing.add(1); // First, during observer() call.
            } else {
                placing.add(2); // Second, due to transaction.
            }
            latch.countDown();
        });

        // Trigger publish due to transaction.
        store.runInTx(() -> putTestEntities(1));

        assertLatchCountedDown(latch, 3);
        subscription.cancel();

        // Second publish request should still deliver second.
        assertEquals(2, placing.size());
        assertEquals(1, (int) placing.get(0));
        assertEquals(2, (int) placing.get(1));
    }

    @Test
    public void testSingle() throws InterruptedException {
        putTestEntitiesScalars();
        int[] valuesInt = {2003, 2007, 2002};
        Query<TestEntity> query = box.query().in(simpleInt, valuesInt).build();

        TestObserver<List<TestEntity>> testObserver = new TestObserver<>();
        query.subscribe().single().observer(testObserver);
        testObserver.assertLatchCountedDown();
        assertEquals(1, testObserver.receivedChanges.size());
        assertEquals(3, testObserver.receivedChanges.get(0).size());

        testObserver.receivedChanges.clear();
        putTestEntities(1);
        Thread.sleep(20);
        assertEquals(0, testObserver.receivedChanges.size());
    }

    @Test
    public void testTransformer() throws InterruptedException {
        int[] valuesInt = {2003, 2007, 2002};
        Query<TestEntity> query = box.query().in(simpleInt, valuesInt).build();
        assertEquals(0, query.count());
        TestObserver<Integer> testObserver = new TestObserver<>();

        query.subscribe().transform(source -> {
            int sum = 0;
            for (TestEntity entity : source) {
                sum += entity.getSimpleInt();
            }
            return sum;
        }).observer(testObserver);
        testObserver.assertLatchCountedDown();

        testObserver.resetLatch();
        putTestEntitiesScalars();
        testObserver.assertLatchCountedDown();
        Thread.sleep(20);

        assertEquals(2, testObserver.receivedChanges.size());
        assertEquals(0, (int) testObserver.receivedChanges.get(0));
        assertEquals(2003 + 2007 + 2002, (int) testObserver.receivedChanges.get(1));
    }

    /**
     * There is an identical test asserting ObjectClassPublisher at
     * {@link ObjectClassObserverTest#transform_inOrderOfPublish()}.
     */
    @Test
    public void transform_inOrderOfPublish() {
        Query<TestEntity> query = box.query().build();

        final CountDownLatch latch = new CountDownLatch(2);
        final AtomicBoolean isLongTransform = new AtomicBoolean(true);
        final List<Integer> placing = new CopyOnWriteArrayList<>();

        // Make first transformation take longer than second.
        DataSubscription subscription = query.subscribe().transform(source -> {
            if (isLongTransform.compareAndSet(true, false)) {
                // Wait long enough so publish triggered by transaction
                // can overtake publish triggered during observer() call.
                Thread.sleep(200);
                return 1; // First, during observer() call.
            }
            return 2; // Second, due to transaction.
        }).observer(data -> {
            placing.add(data);
            latch.countDown();
        });

        // Trigger publish due to transaction.
        store.runInTx(() -> putTestEntities(1));

        assertLatchCountedDown(latch, 3);
        subscription.cancel();

        // Second publish request should still deliver second.
        assertEquals(2, placing.size());
        assertEquals(1, (int) placing.get(0));
        assertEquals(2, (int) placing.get(1));
    }

    @Test
    public void queryCloseWaitsOnPublisher() throws InterruptedException {
        CountDownLatch beforeBlockPublisher = new CountDownLatch(1);
        CountDownLatch blockPublisher = new CountDownLatch(1);
        CountDownLatch beforeQueryClose = new CountDownLatch(1);
        CountDownLatch afterQueryClose = new CountDownLatch(1);

        AtomicBoolean publisherBlocked = new AtomicBoolean(false);
        AtomicBoolean waitedBeforeQueryClose = new AtomicBoolean(false);

        new Thread(() -> {
            Query<TestEntity> query = box.query().build();
            query.subscribe()
                    .onlyChanges() // prevent initial publish call
                    .observer(data -> {
                beforeBlockPublisher.countDown();
                try {
                    publisherBlocked.set(blockPublisher.await(1, TimeUnit.SECONDS));
                } catch (InterruptedException e) {
                    throw new RuntimeException("Observer was interrupted while waiting", e);
                }
            });

            // Trigger the query publisher, prepare so it runs its loop, incl. the query, at least twice
            // and block it from completing the first loop using the observer.
            query.publish();
            query.publish();

            try {
                waitedBeforeQueryClose.set(beforeQueryClose.await(1, TimeUnit.SECONDS));
            } catch (InterruptedException e) {
                throw new RuntimeException("Thread was interrupted while waiting before closing query", e);
            }
            query.close();
            afterQueryClose.countDown();
        }).start();

        // Wait for observer to block the publisher
        assertTrue(beforeBlockPublisher.await(1, TimeUnit.SECONDS));
        // Start closing the query
        beforeQueryClose.countDown();

        // While the publisher is blocked, the query close call should block
        assertFalse(afterQueryClose.await(100, TimeUnit.MILLISECONDS));

        // After the publisher is unblocked and can stop, the query close call should complete
        blockPublisher.countDown();
        assertTrue(afterQueryClose.await(100, TimeUnit.MILLISECONDS));

        // Verify latches were triggered due to reaching 0, not due to timeout
        assertTrue(publisherBlocked.get());
        assertTrue(waitedBeforeQueryClose.get());
    }

    private void putTestEntitiesScalars() {
        putTestEntities(10, null, 2000);
    }

    public static class TestObserver<T> implements DataObserver<T> {

        List<T> receivedChanges = new CopyOnWriteArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        private void log(String message) {
            System.out.println("TestObserver: " + message);
        }

        void printEvents() {
            int count = receivedChanges.size();
            log("Received " + count + " event(s):");
            for (int i = 0; i < count; i++) {
                T receivedChange = receivedChanges.get(i);
                if (receivedChange instanceof List) {
                    List<?> list = (List<?>) receivedChange;
                    log((i + 1) + "/" + count + ": size=" + list.size()
                            + "; items=" + Arrays.toString(list.toArray()));
                }
            }
        }

        void resetLatch() {
            latch = new CountDownLatch(1);
        }

        void assertLatchCountedDown() {
            try {
                assertTrue(latch.await(5, TimeUnit.SECONDS));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            printEvents();
        }

        @Override
        public void onData(T data) {
            receivedChanges.add(data);
            latch.countDown();
        }

    }
}
