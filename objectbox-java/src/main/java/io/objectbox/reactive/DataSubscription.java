package io.objectbox.reactive;

public interface DataSubscription {
    void cancel();

    boolean isCanceled();
}
