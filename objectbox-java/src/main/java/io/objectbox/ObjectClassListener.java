package io.objectbox;

/**
 * Called when objects of a certain type were put or removed in the last committed transaction.
 */
public interface ObjectClassListener<T> {
    void handleChanges(Class<T> objectClass);
}
