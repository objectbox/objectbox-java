package io.objectbox.reactive;

public interface Publisher<T> {
    void subscribe(Observer<T> observer, Object param);

    void unsubscribe(Observer<T> observer, Object param);
}
