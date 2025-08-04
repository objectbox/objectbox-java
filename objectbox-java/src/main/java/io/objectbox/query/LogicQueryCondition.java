/*
 * Copyright 2020 ObjectBox Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
