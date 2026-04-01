/*
 * Copyright 2017 ObjectBox Ltd. All rights reserved.
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

package io.objectbox.android;

import java.util.List;

import androidx.lifecycle.LiveData;
import io.objectbox.query.Query;
import io.objectbox.reactive.DataObserver;
import io.objectbox.reactive.DataSubscription;

/**
 * A {@link LiveData} which allows to observe changes to results of the given query.
 */
public class ObjectBoxLiveData<T> extends LiveData<List<T>> {
    private final Query<T> query;
    private DataSubscription subscription;

    private final DataObserver<List<T>> listener = this::postValue;

    public ObjectBoxLiveData(Query<T> query) {
        this.query = query;
    }

    @Override
    protected void onActive() {
        // called when the LiveData object has an active observer
        if (subscription == null) {
            subscription = query.subscribe().observer(listener);
        }
    }

    @Override
    protected void onInactive() {
        // called when the LiveData object doesn't have any active observers
        if (!hasObservers()) {
            subscription.cancel();
            subscription = null;
        }
    }
}
