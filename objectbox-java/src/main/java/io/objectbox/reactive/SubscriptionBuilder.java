/*
 * Copyright 2017 ObjectBox Ltd.
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

package io.objectbox.reactive;

import javax.annotation.Nullable;

import io.objectbox.annotation.apihint.Internal;
import io.objectbox.query.Query;

/**
 * Builds a {@link DataSubscription} for a {@link DataObserver} passed via {@link #observer(DataObserver)}.
 * Note that the call to {@link #observer(DataObserver)} is mandatory to create the subscription -
 * if you forget it, nothing will happen.
 * <p>
 * When subscribing to a data source such as {@link Query}, this builder allows to configure:
 * <ul>
 * <li>weakly referenced observer via {@link #weak()}</li>
 * <li>a data transform operation via {@link #transform(DataTransformer)}</li>
 * <li>error handlers via {@link #onError(ErrorObserver)}</li>
 * <li>calling the observer using a custom {@link Scheduler} (e.g. Android main thread) via {@link #on(Scheduler)}</li>
 * </ul>
 * <p>
 * Note: the order of methods called in this do not matter.
 * Unlike Rx's Observeable, this builder just collects all info.
 * For example, on(scheduler).transform(transformer) is the same as transform(transformer).on(scheduler).
 *
 * @param <T> The data type the {@link DataObserver} subscribes to.
 */
public class SubscriptionBuilder<T> {
    private final DataPublisher<T> publisher;
    private final Object publisherParam;
    private DataObserver<T> observer;
    //    private Runnable firstRunnable;
    private boolean weak;
    private boolean single;
    private boolean onlyChanges;
    private DataTransformer<T, Object> transformer;
    private Scheduler scheduler;
    private ErrorObserver errorObserver;
    private DataSubscriptionList dataSubscriptionList;
    //    private boolean sync;


    @Internal
    public SubscriptionBuilder(DataPublisher<T> publisher, @Nullable Object param) {
        this.publisher = publisher;
        publisherParam = param;
    }

    //    public Observable<T> runFirst(Runnable firstRunnable) {
    //        if (firstRunnable != null) {
    //            throw new IllegalStateException("Only one asyncRunnable allowed");
    //        }
    //        this.firstRunnable = firstRunnable;
    //        return this;
    //    }

    /**
     * Uses a weak reference for the observer.
     * It is still advised to remove observers explicitly if possible: relying on the garbage collection may cause
     * non-deterministic timing. Until the weak reference is actually cleared by GC, it may still receive notifications.
     */
    public SubscriptionBuilder<T> weak() {
        weak = true;
        return this;
    }

    /**
     * Only deliver the latest data once, do not subscribe for data changes.
     *
     * @see #onlyChanges()
     */
    public SubscriptionBuilder<T> single() {
        single = true;
        return this;
    }

    /**
     * Upon subscribing do not deliver the latest data, only once there are changes.
     *
     * @see #single()
     */
    public SubscriptionBuilder<T> onlyChanges() {
        onlyChanges = true;
        return this;
    }

    //    public Observable<T> sync() {
    //        sync = true;
    //        return this;
    //    }

    /**
     * Transforms the original data from the publisher to some other type.
     * All transformations run sequentially in an asynchronous thread owned by the publisher.
     * <p>
     * This is similar to the map operator of Rx and Kotlin.
     *
     * @param <TO> The type data is transformed to.
     */
    public <TO> SubscriptionBuilder<TO> transform(final DataTransformer<T, TO> transformer) {
        if (this.transformer != null) {
            throw new IllegalStateException("Only one transformer allowed");
        }
        this.transformer = (DataTransformer<T, Object>) transformer;
        return (SubscriptionBuilder<TO>) this;
    }

    /**
     * The given {@link ErrorObserver} is notified when the {@link DataTransformer}
     * ({@link #transform(DataTransformer)}) or {@link DataObserver} ({@link #observer(DataObserver)})
     * threw an exception.
     */
    public SubscriptionBuilder<T> onError(ErrorObserver errorObserver) {
        if (this.errorObserver != null) {
            throw new IllegalStateException("Only one errorObserver allowed");
        }
        this.errorObserver = errorObserver;
        return this;
    }

    /**
     * Changes the thread in which the {@link DataObserver} (and potentially @{@link ErrorObserver}) is called.
     * <p>
     * In the Android package, there is a class AndroidScheduler with a MAIN_THREAD Scheduler implementation.
     * Using MAIN_THREAD, observers will be called in Android's main thread, which is required for UI updates.
     */
    public SubscriptionBuilder<T> on(Scheduler scheduler) {
        if (this.scheduler != null) {
            throw new IllegalStateException("Only one scheduler allowed");
        }
        this.scheduler = scheduler;
        return this;
    }

    /**
     * Completes building the subscription by setting a {@link DataObserver} that receives the data.
     * <p>
     * By default, requests the latest data to be delivered immediately and on any future updates. To change this call
     * {@link #single()} or {@link #onlyChanges()} before.
     * <p>
     * By default, {@link DataObserver#onData(Object)} is called from an internal background thread. Change this by
     * setting a custom scheduler using {@link #on(Scheduler)}. It may also get called for multiple observers at the
     * same time. The order in which observers are called is the same as the subscription order, although this may
     * change in the future.
     * <p>
     * Typically, keep a reference to the returned {@link DataSubscription} to avoid it getting garbage collected, to
     * keep receiving new data.
     * <p>
     * Call {@link DataSubscription#cancel()} once the observer should no longer receive data.
     */
    public DataSubscription observer(DataObserver<T> observer) {
        WeakDataObserver<T> weakObserver = null;
        if (weak) {
            observer = weakObserver = new WeakDataObserver<>(observer);
        }
        this.observer = observer;
        DataSubscriptionImpl subscription = new DataSubscriptionImpl(publisher, publisherParam, observer);
        if (weakObserver != null) {
            weakObserver.setSubscription(subscription);
        }

        if (dataSubscriptionList != null) {
            dataSubscriptionList.add(subscription);
        }

        // TODO FIXME when an observer subscribes twice, it currently won't be added, but we return a new subscription

        // Trivial observers do not have to be wrapped
        if (transformer != null || scheduler != null || errorObserver != null) {
            observer = new ActionObserver(subscription);
        }

        if (single) {
            if (onlyChanges) {
                throw new IllegalStateException("Illegal combination of single() and onlyChanges()");
            }
            publisher.publishSingle(observer, publisherParam);
        } else {
            publisher.subscribe(observer, publisherParam);
            if (!onlyChanges) {
                publisher.publishSingle(observer, publisherParam);
            }
        }
        return subscription;
    }

    public SubscriptionBuilder<T> dataSubscriptionList(DataSubscriptionList dataSubscriptionList) {
        this.dataSubscriptionList = dataSubscriptionList;
        return this;
    }

    /**
     * Wraps a {@link DataObserver} supplied to {@link #observer(DataObserver)} to support result
     * transformation, an error observer or scheduler for result delivery.
     */
    class ActionObserver implements DataObserver<T>, DelegatingObserver<T> {
        private final DataSubscriptionImpl subscription;
        private SchedulerRunOnError schedulerRunOnError;
        private SchedulerRunOnChange schedulerRunOnData;

        public ActionObserver(DataSubscriptionImpl subscription) {
            this.subscription = subscription;
            if (scheduler != null) {
                schedulerRunOnData = new SchedulerRunOnChange();
                if (errorObserver != null) {
                    schedulerRunOnError = new SchedulerRunOnError();
                }
            }
        }

        @Override
        public void onData(final T data) {
            if (transformer != null) {
                transformAndContinue(data);
            } else {
                callOnData(data);
            }
        }

        /**
         * Runs on the thread of the {@link #onData(Object)} caller to ensure data is delivered
         * in the same order as {@link #onData(Object)} was called, to prevent delivering stale data.
         * <p>
         * For both ObjectClassPublisher and QueryPublisher this is the asynchronous
         * thread publish requests are processed on.
         * <p>
         * This could be optimized in the future to allow parallel execution,
         * but this would require an ordering mechanism for the transformed data.
         */
        private void transformAndContinue(final T data) {
            if (subscription.isCanceled()) {
                return;
            }
            try {
                // Type erasure FTW
                T result = (T) transformer.transform(data);
                callOnData(result);
            } catch (Throwable th) {
                callOnError(th, "Transformer failed without an ErrorObserver set");
            }
        }

        private void callOnError(Throwable th, String msgNoErrorObserver) {
            if (errorObserver != null) {
                if (!subscription.isCanceled()) {
                    if (scheduler != null) {
                        scheduler.run(schedulerRunOnError, th);
                    } else {
                        errorObserver.onError(th);
                    }
                }
            } else {
                RuntimeException exception = new RuntimeException(msgNoErrorObserver, th);
                // Might by swallowed by thread pool, so print it right away
                exception.printStackTrace();
                throw exception;
            }
        }

        void callOnData(final T data) {
            if (!subscription.isCanceled()) {
                if (scheduler != null) {
                    scheduler.run(schedulerRunOnData, data);
                } else {
                    try {
                        observer.onData(data);
                    } catch (RuntimeException | Error e) {
                        callOnError(e, "Observer failed without an ErrorObserver set");
                    }
                }
            }
        }

        @Override
        public DataObserver<T> getObserverDelegate() {
            return observer;
        }

        class SchedulerRunOnChange implements RunWithParam<T> {
            @Override
            public void run(T data) {
                if (!subscription.isCanceled()) {
                    try {
                        observer.onData(data);
                    } catch (RuntimeException | Error e) {
                        callOnError(e, "Observer failed without an ErrorObserver set");
                    }
                }
            }
        }

        class SchedulerRunOnError implements RunWithParam<Throwable> {
            @Override
            public void run(Throwable data) {
                if (!subscription.isCanceled()) {
                    errorObserver.onError(data);
                }
            }
        }
    }

}
