package io.objectbox.kotlin

import io.objectbox.Box
import io.objectbox.BoxStore
import kotlin.reflect.KClass

inline fun <reified T> BoxStore.boxFor(): Box<T> = boxFor(T::class.java)

inline fun <T : Any> BoxStore.boxFor(clazz: KClass<T>): Box<T> = boxFor(clazz.java)
