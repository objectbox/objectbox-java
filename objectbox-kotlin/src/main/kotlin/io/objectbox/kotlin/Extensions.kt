/*
 * Copyright 2017 ObjectBox Ltd. All rights reserved.
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
import io.objectbox.BoxStore
import io.objectbox.Property
import io.objectbox.query.QueryBuilder
import kotlin.reflect.KClass

inline fun <reified T> BoxStore.boxFor(): Box<T> = boxFor(T::class.java)

@Suppress("NOTHING_TO_INLINE")
inline fun <T : Any> BoxStore.boxFor(clazz: KClass<T>): Box<T> = boxFor(clazz.java)

/** An alias for the "in" method, which is a reserved keyword in Kotlin. */
inline fun <reified T> QueryBuilder<T>.inValues(property: Property, values: LongArray): QueryBuilder<T>?
        = `in`(property, values)

/** An alias for the "in" method, which is a reserved keyword in Kotlin. */
inline fun <reified T> QueryBuilder<T>.inValues(property: Property, values: IntArray): QueryBuilder<T>?
        = `in`(property, values)
