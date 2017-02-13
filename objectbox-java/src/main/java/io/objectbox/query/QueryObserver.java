package io.objectbox.query;

import java.util.List;

/**
 * Called when resulting objects of a query have potentially changed.
 */
public interface QueryObserver<T> {
    void onQueryChanges(List<T> queryResult);
}
