package io.objectbox.query;

import io.objectbox.Property;

/**
 * Allows building queries with a fluent interface. Use through {@link io.objectbox.Box#query(QueryCondition)}
 * and build a condition with {@link Property} methods.
 */
public interface QueryCondition<T> {

    /**
     * Combines this condition using AND with the given condition.
     *
     * @see #or(QueryCondition)
     */
    QueryCondition<T> and(QueryCondition<T> queryCondition);

    /**
     * Combines this condition using OR with the given condition.
     *
     * @see #and(QueryCondition)
     */
    QueryCondition<T> or(QueryCondition<T> queryCondition);

}
