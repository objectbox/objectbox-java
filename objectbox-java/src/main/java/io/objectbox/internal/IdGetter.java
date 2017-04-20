package io.objectbox.internal;

public interface IdGetter<T> {
    /** Given object must be non-null and have a non-null ID. */
    long getId(T object);
}
