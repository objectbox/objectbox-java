package io.objectbox.rx3

import io.objectbox.query.Query
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single

/**
 * Shortcut for [`RxQuery.flowableOneByOne(query, strategy)`][RxQuery.flowableOneByOne].
 */
fun <T> Query<T>.flowableOneByOne(strategy: BackpressureStrategy = BackpressureStrategy.BUFFER): Flowable<T> {
    return RxQuery.flowableOneByOne(this, strategy)
}

/**
 * Shortcut for [`RxQuery.observable(query)`][RxQuery.observable].
 */
fun <T> Query<T>.observable(): Observable<MutableList<T>> {
    return RxQuery.observable(this)
}

/**
 * Shortcut for [`RxQuery.single(query)`][RxQuery.single].
 */
fun <T> Query<T>.single(): Single<MutableList<T>> {
    return RxQuery.single(this)
}
