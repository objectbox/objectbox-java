package io.objectbox.reactive;

public interface Subscription {
    void cancel();

    boolean isCanceled();
}
