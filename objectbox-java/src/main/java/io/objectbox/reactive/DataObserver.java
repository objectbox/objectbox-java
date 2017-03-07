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
