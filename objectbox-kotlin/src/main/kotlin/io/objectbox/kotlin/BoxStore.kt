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

import io.objectbox.Box
import io.objectbox.BoxStore
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Callable
import java.util.concurrent.ThreadFactory
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.reflect.KClass


/** Shortcut for `BoxStore.boxFor(Entity::class.java)`. */
inline fun <reified T> BoxStore.boxFor(): Box<T> = boxFor(T::class.java)

/** Shortcut for `BoxStore.boxFor(Entity::class.java)`. */
@Suppress("NOTHING_TO_INLINE")
inline fun <T : Any> BoxStore.boxFor(clazz: KClass<T>): Box<T> = boxFor(clazz.java)

/**
 * Wraps [BoxStore.callInTxAsync] in a coroutine that suspends until the transaction has completed.
 * Likewise, on success the return value of the given [callable] is returned, on failure an exception is thrown.
 *
 * Note that even if the coroutine is cancelled the callable is always executed.
 *
 * The callable (and transaction) is submitted to the ObjectBox internal thread pool.
 */
suspend fun <V : Any> BoxStore.awaitCallInTx(callable: Callable<V?>): V? {
    return suspendCoroutine { continuation ->
        callInTxAsync(callable) { result, error ->
            if (error != null) {
                continuation.resumeWithException(error)
            } else {
                continuation.resume(result)
            }
        }
    }
}

/**
 * Creates a coroutine dispatcher backed by a thread pool created with [BoxStore.newCachedThreadPoolExecutor] that
 * automatically cleans up thread-local ObjectBox resources after each task.
 *
 * @return a [kotlinx.coroutines.CoroutineDispatcher] backed by an ObjectBox-aware cached thread pool
 * @see BoxStore.newCachedThreadPoolExecutor
 */
fun BoxStore.newCachedThreadPoolDispatcher() = newCachedThreadPoolExecutor().asCoroutineDispatcher()

/**
 * Creates a coroutine dispatcher backed by a thread pool created with [BoxStore.newCachedThreadPoolExecutor] that
 * automatically cleans up thread-local ObjectBox resources after each task.
 *
 * @return a [kotlinx.coroutines.CoroutineDispatcher] backed by an ObjectBox-aware cached thread pool
 * @see BoxStore.newCachedThreadPoolExecutor
 */
fun BoxStore.newCachedThreadPoolDispatcher(threadFactory: ThreadFactory) =
    newCachedThreadPoolExecutor(threadFactory).asCoroutineDispatcher()

/**
 * Creates a coroutine dispatcher backed by a thread pool created with [BoxStore.newFixedThreadPoolExecutor] that
 * automatically cleans up thread-local ObjectBox resources after each task.
 *
 * @return a [kotlinx.coroutines.CoroutineDispatcher] backed by an ObjectBox-aware fixed thread pool
 * @see BoxStore.newFixedThreadPoolExecutor
 */
fun BoxStore.newFixedThreadPoolDispatcher(nThreads: Int) =
    newFixedThreadPoolExecutor(nThreads).asCoroutineDispatcher()

/**
 * Creates a coroutine dispatcher backed by a thread pool created with [BoxStore.newFixedThreadPoolExecutor] that
 * automatically cleans up thread-local ObjectBox resources after each task.
 *
 * @return a [kotlinx.coroutines.CoroutineDispatcher] backed by an ObjectBox-aware fixed thread pool
 * @see BoxStore.newFixedThreadPoolExecutor
 */
fun BoxStore.newFixedThreadPoolDispatcher(nThreads: Int, threadFactory: ThreadFactory) =
    newFixedThreadPoolExecutor(nThreads, threadFactory).asCoroutineDispatcher()
