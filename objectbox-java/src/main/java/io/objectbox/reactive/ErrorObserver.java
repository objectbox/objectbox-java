package io.objectbox.reactive;

/**
 * Exceptions thrown in {@link DataObserver} and @{@link DataTransformer} can be observed by an error observer set via
 * {@link SubscriptionBuilder#onError(ErrorObserver)}.
 */
public interface ErrorObserver {
    /** Called when an exception was thrown. */
    void onError(Throwable th);
}
