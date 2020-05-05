package io.objectbox.query;

/**
 * Logic based query conditions, currently {@link AndCondition} and {@link OrCondition}.
 */
abstract class LogicQueryCondition<T> extends QueryConditionImpl<T> {

    private final QueryConditionImpl<T> leftCondition;
    private final QueryConditionImpl<T> rightCondition;

    LogicQueryCondition(QueryConditionImpl<T> leftCondition, QueryConditionImpl<T> rightCondition) {
        this.leftCondition = leftCondition;
        this.rightCondition = rightCondition;
    }

    @Override
    void apply(QueryBuilder<T> builder) {
        leftCondition.apply(builder);
        long leftConditionPointer = builder.internalGetLastCondition();

        rightCondition.apply(builder);
        long rightConditionPointer = builder.internalGetLastCondition();

        applyOperator(builder, leftConditionPointer, rightConditionPointer);
    }

    abstract void applyOperator(QueryBuilder<T> builder, long leftCondition, long rightCondition);

    /**
     * Combines the left condition using AND with the right condition.
     */
    static class AndCondition<T> extends LogicQueryCondition<T> {

        AndCondition(QueryConditionImpl<T> leftCondition, QueryConditionImpl<T> rightCondition) {
            super(leftCondition, rightCondition);
        }

        @Override
        void applyOperator(QueryBuilder<T> builder, long leftCondition, long rightCondition) {
            builder.internalAnd(leftCondition, rightCondition);
        }
    }

    /**
     * Combines the left condition using OR with the right condition.
     */
    static class OrCondition<T> extends LogicQueryCondition<T> {

        OrCondition(QueryConditionImpl<T> leftCondition, QueryConditionImpl<T> rightCondition) {
            super(leftCondition, rightCondition);
        }

        @Override
        void applyOperator(QueryBuilder<T> builder, long leftCondition, long rightCondition) {
            builder.internalOr(leftCondition, rightCondition);
        }
    }
}
