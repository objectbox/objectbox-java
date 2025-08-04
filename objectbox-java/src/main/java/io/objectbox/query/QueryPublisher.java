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
 * A {@link DataPublisher} that {@link BoxStore#subscribe(Class) subscribes to the Box} of its associated {@link Query}
 * while there is at least one observer (see {@link #subscribe(DataObserver, Object)} and
 * {@link #unsubscribe(DataObserver, Object)}).
 * <p>
 * Publishing is requested if the Box reports changes, a subscription is
 * {@link SubscriptionBuilder#observer(DataObserver) observed} (if {@link #publishSingle(DataObserver, Object)} is
 * called) or {@link Query#publish()} (calls {@link #publish()}) is called.
 * <p>
 * For publishing the query is re-run and the result data is delivered to the current observers.
 * <p>
 * Data is passed to observers on a single thread ({@link BoxStore#internalScheduleThread(Runnable)}), one at a time, in
 * the order observers were added.
 */
@Internal
class QueryPublisher<T> implements DataPublisher<List<T>>, Runnable {

    /**
     * If enabled, logs states of the publisher runnable. Useful to debug a query subscription.
     */
    public static boolean LOG_STATES = false;
    private final Query<T> query;
    private final Box<T> box;
    private final Set<DataObserver<List<T>>> observers = new CopyOnWriteArraySet<>();
    private final Deque<DataObserver<List<T>>> publishQueue = new ArrayDeque<>();
    private volatile boolean publisherRunning = false;
    private volatile boolean publisherStopped = false;

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
            // Check after obtaining the lock as the publisher may have been stopped while waiting on the lock
            if (publisherStopped) {
                return;
            }
            publishQueue.add(observer);
            if (!publisherRunning) {
                publisherRunning = true;
                box.getStore().internalScheduleThread(this);
            }
        }
    }

    /**
     * Marks this publisher as stopped and if it is currently running waits on it to complete.
     * <p>
     * After calling this, this publisher will no longer run, even if observers subscribe or publishing is requested.
     */
    void stopAndAwait() {
        publisherStopped = true;
        // Doing wait/notify waiting here; could also use the Future from BoxStore.internalScheduleThread() instead.
        // The latter would require another member though, which seems redundant.
        synchronized (this) {
            while (publisherRunning) {
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    if (publisherRunning) {
                        // When called by Query.close() throwing here will leak the query. But not throwing would allow
                        // close() to proceed in destroying the native query while it may still be active (run() of this
                        // is at the query.find() call), which would trigger a VM crash.
                        throw new RuntimeException("Interrupted while waiting for publisher to finish", e);
                    }
                }
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
        log("started");
        try {
            while (!publisherStopped) {
                // Get all queued observer(s), stop processing if none.
                log("checking for observers");
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
                log("running query");
                if (publisherStopped) break;  // Check again to avoid running the query if possible
                List<T> result = query.find();

                // Notify observer(s).
                log("notifying observers");
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
            log("stopped");
            // Re-set if wrapped code throws, otherwise this publisher can no longer publish.
            publisherRunning = false;
            synchronized (this) {
                this.notifyAll();
            }
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

    private static void log(String message) {
        if (LOG_STATES) System.out.println("QueryPublisher: " + message);
    }

}
