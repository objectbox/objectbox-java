package io.objectbox.reactive;

import java.util.List;

public class DataSubscriptionList implements DataSubscription {
    private boolean canceled;
    List<DataSubscription> subscriptions;

    public synchronized void add(DataSubscription subscription) {
        subscriptions.add(subscription);
    }

    @Override
    public synchronized void cancel() {
        canceled = true;
        for (DataSubscription subscription : subscriptions) {
            subscription.cancel();
        }
        subscriptions.clear();
    }

    @Override
    public synchronized boolean isCanceled() {
        return canceled;
    }

    public synchronized int getActiveSubscriptionCount() {
        return subscriptions.size();
    }
}
