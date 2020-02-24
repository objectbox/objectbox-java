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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.annotation.Nullable;

import io.objectbox.Box;
import io.objectbox.BoxStore;
import io.objectbox.annotation.apihint.Internal;
import io.objectbox.reactive.DataObserver;
import io.objectbox.reactive.DataPublisher;
import io.objectbox.reactive.DataPublisherUtils;
import io.objectbox.reactive.DataSubscription;
import io.objectbox.reactive.SubscriptionBuilder;

/**
 * A {@link DataPublisher} that subscribes to an ObjectClassPublisher if there is at least one observer.
 * Publishing is requested if the ObjectClassPublisher reports changes, a subscription is
 * {@link SubscriptionBuilder#observer(DataObserver) observed} or {@link Query#publish()} is called.
 * For publishing the query is re-run and the result delivered to the current observers.
 * Results are published on a single thread, one at a time, in the order publishing was requested.
 */
@Internal
class QueryPublisher<T> implements DataPublisher<List<T>>, Runnable {

    private final Query<T> query;
    private final Box<T> box;
    private final Set<DataObserver<List<T>>> observers = new CopyOnWriteArraySet<>();
    private final Deque<DataObserver<List<T>>> publishQueue = new ArrayDeque<>();
    private volatile boolean publisherRunning = false;

    private static class AllObservers<T> implements DataObserver<List<T>> {
        @Override
        public void onData(List<T> data) {
        }
    }
    /** Placeholder observer if all observers should be notified. */
    private final AllObservers<T> ALL_OBSERVERS = new AllObservers<>();

    private DataObserver<Class<T>> objectClassObserver;
    private DataSubscription objectClassSubscription;

    QueryPublisher(Query<T> query, Box<T> box) {
        this.query = query;
        this.box = box;
    }

    @Override
    public synchronized void subscribe(DataObserver<List<T>> observer, @Nullable Object param) {
        final BoxStore store = box.getStore();
        if (objectClassObserver == null) {
            objectClassObserver = new DataObserver<Class<T>>() {
                @Override
                public void onData(Class<T> objectClass) {
                    publish();
                }
            };
        }
        if (observers.isEmpty()) {
            if (objectClassSubscription != null) {
                throw new IllegalStateException("Existing subscription found");
            }

            // Weak: Query references QueryPublisher, which references objectClassObserver.
            // Query's DataSubscription references QueryPublisher, which references Query.
            // --> Query and its DataSubscription keep objectClassSubscription alive.
            // --> If both are gone, the app could not possibly unsubscribe.
            // --> OK for objectClassSubscription to be GCed and thus unsubscribed?
            // --> However, still subscribed observers to the query will NOT be notified anymore.
            objectClassSubscription = store.subscribe(box.getEntityClass())
                    .weak()
                    .onlyChanges()
                    .observer(objectClassObserver);
        }
        observers.add(observer);
    }

    @Override
    public void publishSingle(DataObserver<List<T>> observer, @Nullable Object param) {
        synchronized (publishQueue) {
            publishQueue.add(observer);
            if (!publisherRunning) {
                publisherRunning = true;
                box.getStore().internalScheduleThread(this);
            }
        }
    }

    void publish() {
        synchronized (publishQueue) {
            publishQueue.add(ALL_OBSERVERS);
            if (!publisherRunning) {
                publisherRunning = true;
                box.getStore().internalScheduleThread(this);
            }
        }
    }

    @Override
    public void run() {
        /*
         * Process publish requests for this query on a single thread to avoid an older request
         * racing a new one (and causing outdated results to be delivered last).
         */
        try {
            while (true) {
                // Get next observer(s).
                DataObserver<List<T>> observer;
                synchronized (publishQueue) {
                    observer = publishQueue.pollFirst();
                    if (observer == null) {
                        publisherRunning = false;
                        break;
                    }
                }

                // Query, then notify observer(s).
                List<T> result = query.find();
                if (ALL_OBSERVERS.equals(observer)) {
                    // Use current list of observers to avoid notifying unsubscribed observers.
                    Set<DataObserver<List<T>>> observers = this.observers;
                    for (DataObserver<List<T>> dataObserver : observers) {
                        dataObserver.onData(result);
                    }
                } else {
                    observer.onData(result);
                }
            }
        } finally {
            // Re-set if wrapped code throws, otherwise this publisher can no longer publish.
            publisherRunning = false;
        }
    }

    @Override
    public synchronized void unsubscribe(DataObserver<List<T>> observer, @Nullable Object param) {
        DataPublisherUtils.removeObserverFromCopyOnWriteSet(observers, observer);
        if (observers.isEmpty()) {
            objectClassSubscription.cancel();
            objectClassSubscription = null;
        }
    }

}
