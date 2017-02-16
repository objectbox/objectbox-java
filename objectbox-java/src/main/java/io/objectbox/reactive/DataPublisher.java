package io.objectbox.reactive;

public interface DataPublisher<T> {
    void subscribe(DataObserver<T> observer, Object param);

    void unsubscribe(DataObserver<T> observer, Object param);
}
