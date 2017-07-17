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
