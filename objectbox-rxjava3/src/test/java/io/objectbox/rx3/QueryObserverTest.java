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

package io.objectbox.rx3;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.objectbox.query.FakeQueryPublisher;
import io.objectbox.query.MockQuery;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Consumer;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * This test has a counterpart in internal integration tests using a real Query and BoxStore.
 */
@RunWith(MockitoJUnitRunner.class)
public class QueryObserverTest {

    private MockQuery<String> mockQuery = new MockQuery<>(false);
    private FakeQueryPublisher<String> publisher = mockQuery.getFakeQueryPublisher();
    private List<String> listResult = new ArrayList<>();

    @Before
    public void prep() {
        listResult.add("foo");
        listResult.add("bar");
    }

    @Test
    public void observable() {
        Observable<List<String>> observable = RxQuery.observable(mockQuery.getQuery());

        // Subscribe should emit.
        TestObserver testObserver = new TestObserver();
        observable.subscribe(testObserver);

        testObserver.assertLatchCountedDown(2);
        assertEquals(1, testObserver.receivedChanges.size());
        assertEquals(0, testObserver.receivedChanges.get(0).size());
        assertNull(testObserver.error);

        // Publish should emit.
        testObserver.resetLatch(1);
        testObserver.receivedChanges.clear();

        publisher.setQueryResult(listResult);
        publisher.publish();

        testObserver.assertLatchCountedDown(5);
        assertEquals(1, testObserver.receivedChanges.size());
        assertEquals(2, testObserver.receivedChanges.get(0).size());

        // Finally, should not be completed.
        assertEquals(0, testObserver.completedCount.get());
    }

    @Test
    public void flowableOneByOne() {
        publisher.setQueryResult(listResult);

        Flowable<String> flowable = RxQuery.flowableOneByOne(mockQuery.getQuery());

        TestObserver testObserver = new TestObserver();
        testObserver.resetLatch(2);
        //noinspection ResultOfMethodCallIgnored
        flowable.subscribe(testObserver);

        testObserver.assertLatchCountedDown(2);
        assertEquals(2, testObserver.receivedChanges.size());
        assertEquals(1, testObserver.receivedChanges.get(0).size());
        assertEquals(1, testObserver.receivedChanges.get(1).size());
        assertNull(testObserver.error);

        testObserver.receivedChanges.clear();

        publisher.publish();
        testObserver.assertNoMoreResults();
    }

    @Test
    public void single() {
        publisher.setQueryResult(listResult);

        Single<List<String>> single = RxQuery.single(mockQuery.getQuery());

        TestObserver testObserver = new TestObserver();
        single.subscribe(testObserver);

        testObserver.assertLatchCountedDown(2);
        assertEquals(1, testObserver.receivedChanges.size());
        assertEquals(2, testObserver.receivedChanges.get(0).size());

        testObserver.receivedChanges.clear();

        publisher.publish();
        testObserver.assertNoMoreResults();
    }

    private static class TestObserver implements Observer<List<String>>, SingleObserver<List<String>>, Consumer<String> {

        List<List<String>> receivedChanges = new CopyOnWriteArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);
        Throwable error;
        AtomicInteger completedCount = new AtomicInteger();

        private void log(String message) {
            System.out.println("TestObserver: " + message);
        }

        void printEvents() {
            int count = receivedChanges.size();
            log("Received " + count + " event(s):");
            for (int i = 0; i < count; i++) {
                List<String> receivedChange = receivedChanges.get(i);
                log((i + 1) + "/" + count + ": size=" + receivedChange.size()
                        + "; items=" + Arrays.toString(receivedChange.toArray()));
            }
        }

        void resetLatch(int count) {
            latch = new CountDownLatch(count);
        }

        void assertLatchCountedDown(int seconds) {
            try {
                assertTrue(latch.await(seconds, TimeUnit.SECONDS));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            printEvents();
        }

        void assertNoMoreResults() {
            assertEquals(0, receivedChanges.size());
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            assertEquals(0, receivedChanges.size());
        }

        @Override
        public void onSubscribe(Disposable d) {
            log("onSubscribe");
        }

        @Override
        public void onNext(List<String> t) {
            log("onNext");
            receivedChanges.add(t);
            latch.countDown();
        }

        @Override
        public void onError(Throwable e) {
            log("onError");
            error = e;
        }

        @Override
        public void onComplete() {
            log("onComplete");
            completedCount.incrementAndGet();
        }

        @Override
        public void accept(String t) {
            log("accept");
            receivedChanges.add(Collections.singletonList(t));
            latch.countDown();
        }

        @Override
        public void onSuccess(List<String> t) {
            log("onSuccess");
            receivedChanges.add(t);
            latch.countDown();
        }
    }
}
