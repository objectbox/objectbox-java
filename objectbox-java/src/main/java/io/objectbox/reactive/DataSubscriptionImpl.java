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

package io.objectbox.reactive;

import javax.annotation.Nullable;

class DataSubscriptionImpl<T> implements DataSubscription {
    private volatile boolean canceled;
    private DataPublisher<T> publisher;
    private Object publisherParam;
    private DataObserver<T> observer;

    DataSubscriptionImpl(DataPublisher<T> publisher, @Nullable Object publisherParam, DataObserver<T> observer) {
        this.publisher = publisher;
        this.publisherParam = publisherParam;
        this.observer = observer;
    }

    @Override
    public synchronized void cancel() {
        canceled = true;
        if (publisher != null) {
            publisher.unsubscribe(observer, publisherParam);

            // Clear out all references, so apps can hold on to subscription object without leaking
            publisher = null;
            observer = null;
            publisherParam = null;
        }
    }

    public boolean isCanceled() {
        return canceled;
    }
}
