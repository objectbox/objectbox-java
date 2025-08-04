/*
 * Copyright 2021 ObjectBox Ltd.
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

import io.objectbox.relation.ToMany

/**
 * Allows making changes (adding and removing entities) to this ToMany with a call to
 * [apply][ToMany.applyChangesToDb] the changes to the database.
 * Can [reset][ToMany.reset] the ToMany before making changes.
 * ```
 * toMany.applyChangesToDb {
 *     add(entity)
 * }
 * ```
 */
@Suppress("unused") // Tested in integration tests
inline fun <T> ToMany<T>.applyChangesToDb(resetFirst: Boolean = false, body: ToMany<T>.() -> Unit) {
    if (resetFirst) reset()
    body()
    applyChangesToDb()
}
