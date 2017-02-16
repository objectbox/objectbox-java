package io.objectbox.reactive;

public interface DataObserver<T> {
    void onData(T data);
}
