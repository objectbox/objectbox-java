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
import java.util.ArrayList;
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

    private static class SubscribedObservers<T> implements DataObserver<List<T>> {
        @Override
        public void onData(List<T> data) {
        }
    }

    /** Placeholder observer to use if all subscribed observers should be notified. */
    private final SubscribedObservers<T> SUBSCRIBED_OBSERVERS = new SubscribedObservers<>();

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
            objectClassObserver = objectClass -> publish();
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
        queueObserverAndScheduleRun(observer);
    }

    void publish() {
        queueObserverAndScheduleRun(SUBSCRIBED_OBSERVERS);
    }

    /**
     * Non-blocking: will just enqueue the changes for a separate thread.
     */
    private void queueObserverAndScheduleRun(DataObserver<List<T>> observer) {
        synchronized (publishQueue) {
            publishQueue.add(observer);
            if (!publisherRunning) {
                publisherRunning = true;
                box.getStore().internalScheduleThread(this);
            }
        }
    }

    /**
     * Processes publish requests for this query on a single thread to prevent
     * older query results getting delivered after newer query results.
     * To speed up processing each loop publishes to all queued observers instead of just the next in line.
     * This reduces time spent querying and waiting for DataObserver.onData() and their potential DataTransformers.
     */
    @Override
    public void run() {
        try {
            while (true) {
                // Get all queued observer(s), stop processing if none.
                List<DataObserver<List<T>>> singlePublishObservers = new ArrayList<>();
                boolean notifySubscribedObservers = false;
                synchronized (publishQueue) {
                    DataObserver<List<T>> nextObserver;
                    while ((nextObserver = publishQueue.poll()) != null) {
                        if (SUBSCRIBED_OBSERVERS.equals(nextObserver)) {
                            notifySubscribedObservers = true;
                        } else {
                            singlePublishObservers.add(nextObserver);
                        }
                    }
                    if (!notifySubscribedObservers && singlePublishObservers.isEmpty()) {
                        publisherRunning = false;
                        break; // Stop.
                    }
                }

                // Query.
                List<T> result = query.find();

                // Notify observer(s).
                for (DataObserver<List<T>> observer : singlePublishObservers) {
                    observer.onData(result);
                }
                if (notifySubscribedObservers) {
                    // Use current list of observers to avoid notifying unsubscribed observers.
                    Set<DataObserver<List<T>>> observers = this.observers;
                    for (DataObserver<List<T>> dataObserver : observers) {
                        dataObserver.onData(result);
                    }
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
