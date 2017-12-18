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

/**
 * Observer that can be subscribed to publishers (e.g. @{@link io.objectbox.BoxStore} and
 * {@link io.objectbox.query.Query}).
 *
 * @param <T> type of data that is observed
 * {@link io.objectbox.query.Query}) to receive data changes.
 */
public interface DataObserver<T> {
    /**
     * Called when data changed.
     * <p>
     * Exception note: if this method throws an exception, it can be reacted on via
     * {@link SubscriptionBuilder#onError(ErrorObserver)}.
     */
    void onData(T data);
}
