package io.objectbox.query;

/**
 * Decides which entities to keep as a query result.
 *
 * @param <T> The entity
 */
public interface QueryFilter<T> {
    boolean keep(T entity);
}
