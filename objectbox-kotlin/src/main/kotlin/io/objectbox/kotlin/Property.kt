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

@file:Suppress("unused") // Public API.

package io.objectbox.kotlin

import io.objectbox.Property
import io.objectbox.query.PropertyQueryCondition
import java.util.*


// Boolean
/** Creates an "equal ('=')" condition for this property. */
infix fun <T> Property<T>.equal(value: Boolean): PropertyQueryCondition<T> {
    return equal(value)
}

/** Creates a "not equal ('&lt;&gt;')" condition for this property. */
infix fun <T> Property<T>.notEqual(value: Boolean): PropertyQueryCondition<T> {
    return notEqual(value)
}

// Short
/** Creates an "equal ('=')" condition for this property. */
infix fun <T> Property<T>.equal(value: Short): PropertyQueryCondition<T> {
    return equal(value)
}

/** Creates a "not equal ('&lt;&gt;')" condition for this property. */
infix fun <T> Property<T>.notEqual(value: Short): PropertyQueryCondition<T> {
    return notEqual(value)
}

/** Creates a "greater than ('&gt;')" condition for this property. */
infix fun <T> Property<T>.greater(value: Short): PropertyQueryCondition<T> {
    return greater(value)
}

/** Creates a "less than ('&lt;')" condition for this property. */
infix fun <T> Property<T>.less(value: Short): PropertyQueryCondition<T> {
    return less(value)
}

// Int
/** Creates an "equal ('=')" condition for this property. */
infix fun <T> Property<T>.equal(value: Int): PropertyQueryCondition<T> {
    return equal(value)
}

/** Creates a "not equal ('&lt;&gt;')" condition for this property. */
infix fun <T> Property<T>.notEqual(value: Int): PropertyQueryCondition<T> {
    return notEqual(value)
}

/** Creates a "greater than ('&gt;')" condition for this property. */
infix fun <T> Property<T>.greater(value: Int): PropertyQueryCondition<T> {
    return greater(value)
}

/** Creates a "less than ('&lt;')" condition for this property. */
infix fun <T> Property<T>.less(value: Int): PropertyQueryCondition<T> {
    return less(value)
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
infix fun <T> Property<T>.equal(value: Long): PropertyQueryCondition<T> {
    return equal(value)
}

/** Creates a "not equal ('&lt;&gt;')" condition for this property. */
infix fun <T> Property<T>.notEqual(value: Long): PropertyQueryCondition<T> {
    return notEqual(value)
}

/** Creates a "greater than ('&gt;')" condition for this property. */
infix fun <T> Property<T>.greater(value: Long): PropertyQueryCondition<T> {
    return greater(value)
}

/** Creates a "less than ('&lt;')" condition for this property. */
infix fun <T> Property<T>.less(value: Long): PropertyQueryCondition<T> {
    return less(value)
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
infix fun <T> Property<T>.greater(value: Double): PropertyQueryCondition<T> {
    return greater(value)
}

/** Creates a "less than ('&lt;')" condition for this property. */
infix fun <T> Property<T>.less(value: Double): PropertyQueryCondition<T> {
    return less(value)
}

// Date
/** Creates an "equal ('=')" condition for this property. */
infix fun <T> Property<T>.equal(value: Date): PropertyQueryCondition<T> {
    return equal(value)
}

/** Creates a "not equal ('&lt;&gt;')" condition for this property. */
infix fun <T> Property<T>.notEqual(value: Date): PropertyQueryCondition<T> {
    return notEqual(value)
}

/** Creates a "greater than ('&gt;')" condition for this property. */
infix fun <T> Property<T>.greater(value: Date): PropertyQueryCondition<T> {
    return greater(value)
}

/** Creates a "less than ('&lt;')" condition for this property. */
infix fun <T> Property<T>.less(value: Date): PropertyQueryCondition<T> {
    return less(value)
}

// String
/** Creates an "equal ('=')" condition for this property. */
infix fun <T> Property<T>.equal(value: String): PropertyQueryCondition<T> {
    return equal(value)
}

/** Creates a "not equal ('&lt;&gt;')" condition for this property. */
infix fun <T> Property<T>.notEqual(value: String): PropertyQueryCondition<T> {
    return notEqual(value)
}

/** Creates a "greater than ('&gt;')" condition for this property. */
infix fun <T> Property<T>.greater(value: String): PropertyQueryCondition<T> {
    return greater(value)
}

/** Creates a "less than ('&lt;')" condition for this property. */
infix fun <T> Property<T>.less(value: String): PropertyQueryCondition<T> {
    return less(value)
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
infix fun <T> Property<T>.equal(value: ByteArray): PropertyQueryCondition<T> {
    return equal(value)
}

/** Creates a "greater than ('&gt;')" condition for this property. */
infix fun <T> Property<T>.greater(value: ByteArray): PropertyQueryCondition<T> {
    return greater(value)
}

/** Creates a "less than ('&lt;')" condition for this property. */
infix fun <T> Property<T>.less(value: ByteArray): PropertyQueryCondition<T> {
    return less(value)
}
