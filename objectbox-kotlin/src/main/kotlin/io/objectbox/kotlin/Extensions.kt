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

@file:Suppress("unused") // tested in integration test project

package io.objectbox.kotlin

import io.objectbox.Box
import io.objectbox.BoxStore
import io.objectbox.Property
import io.objectbox.query.Query
import io.objectbox.query.QueryBuilder
import io.objectbox.relation.ToMany
import kotlin.reflect.KClass

/** Shortcut for `BoxStore.boxFor(Entity::class.java)`. */
inline fun <reified T> BoxStore.boxFor(): Box<T> = boxFor(T::class.java)

/** Shortcut for `BoxStore.boxFor(Entity::class.java)`. */
@Suppress("NOTHING_TO_INLINE")
inline fun <T : Any> BoxStore.boxFor(clazz: KClass<T>): Box<T> = boxFor(clazz.java)

/** An alias for the "in" method, which is a reserved keyword in Kotlin. */
inline fun <reified T> QueryBuilder<T>.inValues(property: Property<T>, values: LongArray): QueryBuilder<T>
        = `in`(property, values)

/** An alias for the "in" method, which is a reserved keyword in Kotlin. */
inline fun <reified T> QueryBuilder<T>.inValues(property: Property<T>, values: IntArray): QueryBuilder<T>
        = `in`(property, values)

/** An alias for the "in" method, which is a reserved keyword in Kotlin. */
inline fun <reified T> QueryBuilder<T>.inValues(property: Property<T>, values: Array<String>): QueryBuilder<T>
        = `in`(property, values)

/** An alias for the "in" method, which is a reserved keyword in Kotlin. */
inline fun <reified T> QueryBuilder<T>.inValues(property: Property<T>, values: Array<String>,
                                                stringOrder: QueryBuilder.StringOrder): QueryBuilder<T>
        = `in`(property, values, stringOrder)

// Shortcuts for Short

/** Shortcut for [equal(property, value.toLong())][QueryBuilder.equal] */
inline fun <reified T> QueryBuilder<T>.equal(property: Property<T>, value: Short): QueryBuilder<T> {
    return equal(property, value.toLong())
}

/** Shortcut for [notEqual(property, value.toLong())][QueryBuilder.notEqual] */
inline fun <reified T> QueryBuilder<T>.notEqual(property: Property<T>, value: Short): QueryBuilder<T> {
    return notEqual(property, value.toLong())
}

/** Shortcut for [less(property, value.toLong())][QueryBuilder.less] */
inline fun <reified T> QueryBuilder<T>.less(property: Property<T>, value: Short): QueryBuilder<T> {
    return less(property, value.toLong())
}

/** Shortcut for [greater(property, value.toLong())][QueryBuilder.greater] */
inline fun <reified T> QueryBuilder<T>.greater(property: Property<T>, value: Short): QueryBuilder<T> {
    return greater(property, value.toLong())
}

/** Shortcut for [between(property, value1.toLong(), value2.toLong())][QueryBuilder.between] */
inline fun <reified T> QueryBuilder<T>.between(property: Property<T>, value1: Short, value2: Short): QueryBuilder<T> {
    return between(property, value1.toLong(), value2.toLong())
}

// Shortcuts for Int

/** Shortcut for [equal(property, value.toLong())][QueryBuilder.equal] */
inline fun <reified T> QueryBuilder<T>.equal(property: Property<T>, value: Int): QueryBuilder<T> {
    return equal(property, value.toLong())
}

/** Shortcut for [notEqual(property, value.toLong())][QueryBuilder.notEqual] */
inline fun <reified T> QueryBuilder<T>.notEqual(property: Property<T>, value: Int): QueryBuilder<T> {
    return notEqual(property, value.toLong())
}

/** Shortcut for [less(property, value.toLong())][QueryBuilder.less] */
inline fun <reified T> QueryBuilder<T>.less(property: Property<T>, value: Int): QueryBuilder<T> {
    return less(property, value.toLong())
}

/** Shortcut for [greater(property, value.toLong())][QueryBuilder.greater] */
inline fun <reified T> QueryBuilder<T>.greater(property: Property<T>, value: Int): QueryBuilder<T> {
    return greater(property, value.toLong())
}

/** Shortcut for [between(property, value1.toLong(), value2.toLong())][QueryBuilder.between] */
inline fun <reified T> QueryBuilder<T>.between(property: Property<T>, value1: Int, value2: Int): QueryBuilder<T> {
    return between(property, value1.toLong(), value2.toLong())
}

// Shortcuts for Float

/** Shortcut for [equal(property, value.toDouble(), tolerance.toDouble())][QueryBuilder.equal] */
inline fun <reified T> QueryBuilder<T>.equal(property: Property<T>, value: Float, tolerance: Float): QueryBuilder<T> {
    return equal(property, value.toDouble(), tolerance.toDouble())
}

/** Shortcut for [less(property, value.toDouble())][QueryBuilder.less] */
inline fun <reified T> QueryBuilder<T>.less(property: Property<T>, value: Float): QueryBuilder<T> {
    return less(property, value.toDouble())
}

/** Shortcut for [greater(property, value.toDouble())][QueryBuilder.greater] */
inline fun <reified T> QueryBuilder<T>.greater(property: Property<T>, value: Float): QueryBuilder<T> {
    return greater(property, value.toDouble())
}

/** Shortcut for [between(property, value1.toDouble(), value2.toDouble())][QueryBuilder.between] */
inline fun <reified T> QueryBuilder<T>.between(property: Property<T>, value1: Float, value2: Float): QueryBuilder<T> {
    return between(property, value1.toDouble(), value2.toDouble())
}

/**
 * Allows building a query for this Box instance with a call to [build][QueryBuilder.build] to return a [Query] instance.
 * ```
 * val query = box.query {
 *     equal(Entity_.property, value)
 * }
 * ```
 */
inline fun <T> Box<T>.query(block: QueryBuilder<T>.() -> Unit) : Query<T> {
    val builder = query()
    block(builder)
    return builder.build()
}

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
inline fun <T> ToMany<T>.applyChangesToDb(resetFirst: Boolean = false, body: ToMany<T>.() -> Unit) {
    if (resetFirst) reset()
    body()
    applyChangesToDb()
}
