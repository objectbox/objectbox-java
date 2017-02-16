package io.objectbox.reactive;

class SubscriptionImpl<T> implements Subscription {
    private volatile boolean canceled;
    private Publisher<T> publisher;
    private Object publisherParam;
    private Observer<T> observer;

    SubscriptionImpl(Publisher<T> publisher, Object publisherParam, Observer<T> observer) {
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
