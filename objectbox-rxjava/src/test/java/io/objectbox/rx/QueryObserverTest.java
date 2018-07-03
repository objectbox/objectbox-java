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

package io.objectbox.rx;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.objectbox.query.FakeQueryPublisher;
import io.objectbox.query.MockQuery;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class QueryObserverTest implements Observer<List<String>>, SingleObserver<List<String>>, Consumer<String> {

    private List<List<String>> receivedChanges = new CopyOnWriteArrayList<>();
    private CountDownLatch latch = new CountDownLatch(1);

    private MockQuery<String> mockQuery = new MockQuery<>(false);
    private FakeQueryPublisher<String> publisher = mockQuery.getFakeQueryPublisher();
    private List<String> listResult = new ArrayList<>();
    private Throwable error;

    private AtomicInteger completedCount = new AtomicInteger();

    @Before
    public void prep() {
        listResult.add("foo");
        listResult.add("bar");
    }

    @Test
    public void testObservable() {
        Observable observable = RxQuery.observable(mockQuery.getQuery());
        observable.subscribe((Observer) this);
        assertLatchCountedDown(latch, 2);
        assertEquals(1, receivedChanges.size());
        assertEquals(0, receivedChanges.get(0).size());
        assertNull(error);

        latch = new CountDownLatch(1);
        receivedChanges.clear();
        publisher.setQueryResult(listResult);
        publisher.publish();

        assertLatchCountedDown(latch, 5);
        assertEquals(1, receivedChanges.size());
        assertEquals(2, receivedChanges.get(0).size());

        assertEquals(0, completedCount.get());

        //Unsubscribe?
        //        receivedChanges.clear();
        //        latch = new CountDownLatch(1);
        //        assertLatchCountedDown(latch, 5);
        //
        //        assertEquals(1, receivedChanges.size());
        //        assertEquals(3, receivedChanges.get(0).size());
    }

    @Test
    public void testFlowableOneByOne() {
        publisher.setQueryResult(listResult);

        latch = new CountDownLatch(2);
        Flowable flowable = RxQuery.flowableOneByOne(mockQuery.getQuery());
        flowable.subscribe(this);
        assertLatchCountedDown(latch, 2);
        assertEquals(2, receivedChanges.size());
        assertEquals(1, receivedChanges.get(0).size());
        assertEquals(1, receivedChanges.get(1).size());
        assertNull(error);

        receivedChanges.clear();
        publisher.publish();
        assertNoMoreResults();
    }

    @Test
    public void testSingle() {
        publisher.setQueryResult(listResult);
        Single single = RxQuery.single(mockQuery.getQuery());
        single.subscribe((SingleObserver) this);
        assertLatchCountedDown(latch, 2);
        assertEquals(1, receivedChanges.size());
        assertEquals(2, receivedChanges.get(0).size());

        receivedChanges.clear();
        publisher.publish();
        assertNoMoreResults();
    }

    protected void assertNoMoreResults() {
        assertEquals(0, receivedChanges.size());
        try {
            Thread.sleep(20);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        assertEquals(0, receivedChanges.size());
    }

    protected void assertLatchCountedDown(CountDownLatch latch, int seconds) {
        try {
            assertTrue(latch.await(seconds, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onSubscribe(Disposable d) {

    }

    @Override
    public void onSuccess(List<String> queryResult) {
        receivedChanges.add(queryResult);
        latch.countDown();
    }

    @Override
    public void onNext(List<String> queryResult) {
        receivedChanges.add(queryResult);
        latch.countDown();
    }

    @Override
    public void onError(Throwable e) {
        error = e;
    }

    @Override
    public void onComplete() {
        completedCount.incrementAndGet();
    }

    @Override
    public void accept(@NonNull String s) throws Exception {
        receivedChanges.add(Collections.singletonList(s));
        latch.countDown();
    }
}
