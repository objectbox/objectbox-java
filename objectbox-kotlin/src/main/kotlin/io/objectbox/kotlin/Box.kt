/*
 * Copyright 2021-2025 ObjectBox Ltd.
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

import io.objectbox.Box
import io.objectbox.query.Query
import io.objectbox.query.QueryBuilder


/**
 * Note: new code should use the [Box.query] functions directly, including the new query API.
 *
 * Allows building a query for this Box instance with a call to [build][QueryBuilder.build] to return a [Query] instance.
 *
 * ```
 * val query = box.query {
 *     equal(Entity_.property, value)
 * }
 * ```
 */
@Deprecated("New code should use query(queryCondition).build() instead.")
inline fun <T> Box<T>.query(block: QueryBuilder<T>.() -> Unit): Query<T> {
    val builder = query()
    block(builder)
    return builder.build()
}
