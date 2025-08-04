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

package io.objectbox.kotlin

import io.objectbox.query.QueryCondition


/**
 * Combines the left hand side condition using AND with the right hand side condition.
 *
 * @see or
 */
infix fun <T> QueryCondition<T>.and(queryCondition: QueryCondition<T>): QueryCondition<T> {
    return and(queryCondition)
}

/**
 * Combines the left hand side condition using OR with the right hand side condition.
 *
 * @see and
 */
infix fun <T> QueryCondition<T>.or(queryCondition: QueryCondition<T>): QueryCondition<T> {
    return or(queryCondition)
}