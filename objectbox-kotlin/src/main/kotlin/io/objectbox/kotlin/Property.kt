/*
 * Copyright 2020 ObjectBox Ltd. All rights reserved.
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

@file:Suppress("unused") // Public API.

package io.objectbox.kotlin

import io.objectbox.Property
import io.objectbox.query.PropertyQueryCondition
import java.util.*


// Boolean
/** Creates an "equal ('=')" condition for this property. */
infix fun <T> Property<T>.eq(value: Boolean): PropertyQueryCondition<T> {
    return eq(value)
}

/** Creates a "not equal ('&lt;&gt;')" condition for this property. */
infix fun <T> Property<T>.notEq(value: Boolean): PropertyQueryCondition<T> {
    return notEq(value)
}

// Short
/** Creates an "equal ('=')" condition for this property. */
infix fun <T> Property<T>.eq(value: Short): PropertyQueryCondition<T> {
    return eq(value)
}

/** Creates a "not equal ('&lt;&gt;')" condition for this property. */
infix fun <T> Property<T>.notEq(value: Short): PropertyQueryCondition<T> {
    return notEq(value)
}

/** Creates a "greater than ('&gt;')" condition for this property. */
infix fun <T> Property<T>.gt(value: Short): PropertyQueryCondition<T> {
    return gt(value)
}

/** Creates a "less than ('&lt;')" condition for this property. */
infix fun <T> Property<T>.lt(value: Short): PropertyQueryCondition<T> {
    return lt(value)
}

// Int
/** Creates an "equal ('=')" condition for this property. */
infix fun <T> Property<T>.eq(value: Int): PropertyQueryCondition<T> {
    return eq(value)
}

/** Creates a "not equal ('&lt;&gt;')" condition for this property. */
infix fun <T> Property<T>.notEq(value: Int): PropertyQueryCondition<T> {
    return notEq(value)
}

/** Creates a "greater than ('&gt;')" condition for this property. */
infix fun <T> Property<T>.gt(value: Int): PropertyQueryCondition<T> {
    return gt(value)
}

/** Creates a "less than ('&lt;')" condition for this property. */
infix fun <T> Property<T>.lt(value: Int): PropertyQueryCondition<T> {
    return lt(value)
}

// IntArray
/** Creates an "IN (..., ..., ...)" condition for this property. */
infix fun <T> Property<T>.oneOf(value: IntArray): PropertyQueryCondition<T> {
    return oneOf(value)
}

/** Creates a "NOT IN (..., ..., ...)" condition for this property. */
infix fun <T> Property<T>.notOneOf(value: IntArray): PropertyQueryCondition<T> {
    return notOneOf(value)
}

// Long
/** Creates an "equal ('=')" condition for this property. */
infix fun <T> Property<T>.eq(value: Long): PropertyQueryCondition<T> {
    return eq(value)
}

/** Creates a "not equal ('&lt;&gt;')" condition for this property. */
infix fun <T> Property<T>.notEq(value: Long): PropertyQueryCondition<T> {
    return notEq(value)
}

/** Creates a "greater than ('&gt;')" condition for this property. */
infix fun <T> Property<T>.gt(value: Long): PropertyQueryCondition<T> {
    return gt(value)
}

/** Creates a "less than ('&lt;')" condition for this property. */
infix fun <T> Property<T>.lt(value: Long): PropertyQueryCondition<T> {
    return lt(value)
}

// LongArray
/** Creates an "IN (..., ..., ...)" condition for this property. */
infix fun <T> Property<T>.oneOf(value: LongArray): PropertyQueryCondition<T> {
    return oneOf(value)
}

/** Creates a "NOT IN (..., ..., ...)" condition for this property. */
infix fun <T> Property<T>.notOneOf(value: LongArray): PropertyQueryCondition<T> {
    return notOneOf(value)
}

// Double
/** Creates a "greater than ('&gt;')" condition for this property. */
infix fun <T> Property<T>.gt(value: Double): PropertyQueryCondition<T> {
    return gt(value)
}

/** Creates a "less than ('&lt;')" condition for this property. */
infix fun <T> Property<T>.lt(value: Double): PropertyQueryCondition<T> {
    return lt(value)
}

// Date
/** Creates an "equal ('=')" condition for this property. */
infix fun <T> Property<T>.eq(value: Date): PropertyQueryCondition<T> {
    return eq(value)
}

/** Creates a "not equal ('&lt;&gt;')" condition for this property. */
infix fun <T> Property<T>.notEq(value: Date): PropertyQueryCondition<T> {
    return notEq(value)
}

/** Creates a "greater than ('&gt;')" condition for this property. */
infix fun <T> Property<T>.gt(value: Date): PropertyQueryCondition<T> {
    return gt(value)
}

/** Creates a "less than ('&lt;')" condition for this property. */
infix fun <T> Property<T>.lt(value: Date): PropertyQueryCondition<T> {
    return lt(value)
}

// String
/** Creates an "equal ('=')" condition for this property. */
infix fun <T> Property<T>.eq(value: String): PropertyQueryCondition<T> {
    return eq(value)
}

/** Creates a "not equal ('&lt;&gt;')" condition for this property. */
infix fun <T> Property<T>.notEq(value: String): PropertyQueryCondition<T> {
    return notEq(value)
}

/** Creates a "greater than ('&gt;')" condition for this property. */
infix fun <T> Property<T>.gt(value: String): PropertyQueryCondition<T> {
    return gt(value)
}

/** Creates a "less than ('&lt;')" condition for this property. */
infix fun <T> Property<T>.lt(value: String): PropertyQueryCondition<T> {
    return lt(value)
}

infix fun <T> Property<T>.contains(value: String): PropertyQueryCondition<T> {
    return contains(value)
}

infix fun <T> Property<T>.startsWith(value: String): PropertyQueryCondition<T> {
    return startsWith(value)
}

infix fun <T> Property<T>.endsWith(value: String): PropertyQueryCondition<T> {
    return endsWith(value)
}

// Array<String>
/** Creates an "IN (..., ..., ...)" condition for this property. */
infix fun <T> Property<T>.oneOf(value: Array<String>): PropertyQueryCondition<T> {
    return oneOf(value)
}

// ByteArray
/** Creates an "equal ('=')" condition for this property. */
infix fun <T> Property<T>.eq(value: ByteArray): PropertyQueryCondition<T> {
    return eq(value)
}

/** Creates a "greater than ('&gt;')" condition for this property. */
infix fun <T> Property<T>.gt(value: ByteArray): PropertyQueryCondition<T> {
    return gt(value)
}

/** Creates a "less than ('&lt;')" condition for this property. */
infix fun <T> Property<T>.lt(value: ByteArray): PropertyQueryCondition<T> {
    return lt(value)
}
