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
