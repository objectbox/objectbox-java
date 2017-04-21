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
