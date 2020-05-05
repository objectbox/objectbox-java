package io.objectbox.query;

import io.objectbox.query.LogicQueryCondition.AndCondition;
import io.objectbox.query.LogicQueryCondition.OrCondition;

/**
 * Hides the {@link #apply(QueryBuilder)} method from the public API ({@link QueryCondition}).
 */
abstract class QueryConditionImpl<T> implements QueryCondition<T> {

    @Override
    public QueryCondition<T> and(QueryCondition<T> queryCondition) {
        return new AndCondition<>(this, (QueryConditionImpl<T>) queryCondition);
    }

    @Override
    public QueryCondition<T> or(QueryCondition<T> queryCondition) {
        return new OrCondition<>(this, (QueryConditionImpl<T>) queryCondition);
    }

    abstract void apply(QueryBuilder<T> builder);
}
