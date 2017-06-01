package io.objectbox

inline fun <reified T> BoxStore.boxFor(): Box<T> = boxFor(T::class.java)
