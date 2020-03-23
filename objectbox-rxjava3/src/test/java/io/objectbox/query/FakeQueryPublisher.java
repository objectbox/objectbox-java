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

import io.objectbox.reactive.DataObserver;
import io.objectbox.reactive.DataPublisher;
import io.objectbox.reactive.DataPublisherUtils;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class FakeQueryPublisher<T> implements DataPublisher<List<T>> {

    private final Set<DataObserver<List<T>>> observers = new CopyOnWriteArraySet();

    private List<T> queryResult = Collections.emptyList();

    public List<T> getQueryResult() {
        return queryResult;
    }

    public void setQueryResult(List<T> queryResult) {
        this.queryResult = queryResult;
    }

    @Override
    public synchronized void subscribe(DataObserver<List<T>> observer, Object param) {
        observers.add(observer);
    }

    @Override
    public void publishSingle(final DataObserver<List<T>> observer, Object param) {
        observer.onData(queryResult);
    }

    public void publish() {
        for (DataObserver<List<T>> observer : observers) {
            observer.onData(queryResult);
        }
    }

    @Override
    public synchronized void unsubscribe(DataObserver<List<T>> observer, Object param) {
        DataPublisherUtils.removeObserverFromCopyOnWriteSet(observers, observer);
    }

}
