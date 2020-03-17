package io.objectbox.query;

import io.objectbox.Property;

/**
 * A condition on a {@link Property}, which can have an alias to allow referring to it later.
 */
public interface PropertyQueryCondition<T> extends QueryCondition<T> {

    /**
     * Assigns an alias to this condition that can later be used with the {@link Query} setParameter methods.
     */
    QueryCondition<T> alias(String name);

}
