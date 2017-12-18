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

import java.util.ArrayList;
import java.util.List;

/**
 * Tracks any number of {@link DataSubscription} objects, which can be canceled with a single {@link #cancel()} call.
 * This is typically used in live cycle components like Android's Activity:
 * <ul>
 * <li>Make DataSubscriptionList a field</li>
 * <li>Call {@link #add(DataSubscription)} during onStart/onResume for each subscription</li>
 * <li>Call {@link #cancel()} during onStop/onPause</li>
 * </ul>
 */
public class DataSubscriptionList implements DataSubscription {
    private final List<DataSubscription> subscriptions = new ArrayList<>();
    private boolean canceled;

    /** Add the given subscription to the list of tracked subscriptions. Clears any previous "canceled" state. */
    public synchronized void add(DataSubscription subscription) {
        subscriptions.add(subscription);
        canceled = false;
    }

    /** Cancels all tracked subscriptions and removes all references to them. */
    @Override
    public synchronized void cancel() {
        canceled = true;
        for (DataSubscription subscription : subscriptions) {
            subscription.cancel();
        }
        subscriptions.clear();
    }

    /** Returns true if {@link #cancel()} was called without any subsequent calls to {@link #add(DataSubscription)}. */
    @Override
    public synchronized boolean isCanceled() {
        return canceled;
    }

    /** Returns number of active (added) subscriptions (resets to 0 after {@link #cancel()}). */
    public synchronized int getActiveSubscriptionCount() {
        return subscriptions.size();
    }
}
