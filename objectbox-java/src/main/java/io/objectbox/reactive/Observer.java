package io.objectbox.reactive;

public interface Observer<T> {
    void onChange(T data);
}
