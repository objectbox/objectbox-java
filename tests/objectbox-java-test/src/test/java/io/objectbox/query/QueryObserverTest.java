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

package io.objectbox.query;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.objectbox.AbstractObjectBoxTest;
import io.objectbox.Box;
import io.objectbox.TestEntity;
import io.objectbox.reactive.DataObserver;


import static io.objectbox.TestEntity_.simpleInt;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class QueryObserverTest extends AbstractObjectBoxTest {

    private Box<TestEntity> box;

    @Before
    public void setUpBox() {
        box = getTestEntityBox();
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
