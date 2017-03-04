package io.objectbox.query;

public interface QueryConsumer<T> {
    void accept(T data);
}
