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
