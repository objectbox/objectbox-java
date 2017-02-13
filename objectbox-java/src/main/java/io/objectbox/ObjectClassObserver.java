package io.objectbox;

/**
 * Called when objects of a certain type were put or removed in the last committed transaction.
 */
public interface ObjectClassObserver<T> {
    void onChanges(Class<T> objectClass);
}
