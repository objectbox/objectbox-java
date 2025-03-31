package io.objectbox.kotlin

import io.objectbox.BoxStore
import io.objectbox.query.Query
import io.objectbox.reactive.SubscriptionBuilder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow


/**
 * Like [SubscriptionBuilder.observer], but emits data changes to the returned flow.
 * Automatically cancels the subscription when the flow is canceled.
 *
 * For example to create a flow to listen to all changes to a box:
 * ```
 * store.subscribe(TestEntity::class.java).toFlow()
 * ```
 *
 * Or to get the latest query results on any changes to a box:
 * ```
 * box.query().subscribe().toFlow()
 * ```
 */
@ExperimentalCoroutinesApi
fun <T> SubscriptionBuilder<T>.toFlow(): Flow<T> = callbackFlow {
    val subscription = this@toFlow.observer {
            trySendBlocking(it)
        }
    awaitClose { subscription.cancel() }
}

/**
 * Shortcut for `BoxStore.subscribe(forClass).toFlow()`, see [BoxStore.subscribe] and [toFlow] for details.
 */
@ExperimentalCoroutinesApi
fun <T> BoxStore.flow(forClass: Class<T>): Flow<Class<T>> = this.subscribe(forClass).toFlow()

/**
 * Shortcut for `query.subscribe().toFlow()`, see [Query.subscribe] and [toFlow] for details.
 */
@ExperimentalCoroutinesApi
fun <T> Query<T>.flow(): Flow<MutableList<T>> = this@flow.subscribe().toFlow()