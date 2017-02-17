package io.objectbox.reactive;

/**
 * The result of subscribing an @{@link DataObserver} using @{@link SubscriptionBuilder#observer(DataObserver)}.
 * Used to cancel the subscription (unsubscribe).
 */
public interface DataSubscription {
    /** The Observer shall not receive anymore updates. */
    void cancel();

    /** Current cancellation state of the subscription. */
    boolean isCanceled();
}
