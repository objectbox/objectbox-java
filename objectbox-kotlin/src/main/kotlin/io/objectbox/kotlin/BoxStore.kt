/*
 * Copyright 2021 ObjectBox Ltd. All rights reserved.
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
import java.util.concurrent.Callable
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
