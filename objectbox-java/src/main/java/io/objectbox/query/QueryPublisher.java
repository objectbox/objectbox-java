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

@Internal
class QueryPublisher<T> implements DataPublisher<List<T>> {

    private final Query<T> query;
    private final Box<T> box;
    private final Set<DataObserver<List<T>>> observers = new CopyOnWriteArraySet<>();

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
    public void publishSingle(final DataObserver<List<T>> observer, @Nullable Object param) {
        box.getStore().internalScheduleThread(new Runnable() {
            @Override
            public void run() {
                List<T> result = query.find();
                observer.onData(result);
            }
        });
    }

    void publish() {
        box.getStore().internalScheduleThread(new Runnable() {
            @Override
            public void run() {
                List<T> result = query.find();
                for (DataObserver<List<T>> observer : observers) {
                    observer.onData(result);
                }
            }
        });
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
