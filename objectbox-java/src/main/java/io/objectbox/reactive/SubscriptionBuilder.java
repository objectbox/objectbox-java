package io.objectbox.reactive;

import java.util.concurrent.ExecutorService;

public class SubscriptionBuilder<T> {
    private Publisher<T> publisher;
    private final Object publisherParam;
    private final ExecutorService threadPool;
    private Observer<T> observer;
    //    private Runnable firstRunnable;
    private boolean weak;
    private Transformer<T, Object> transformer;
    private Scheduler scheduler;
    private ErrorObserver errorObserver;
//    private boolean sync;


    public SubscriptionBuilder(Publisher<T> publisher, Object param, ExecutorService threadPool) {
        this.publisher = publisher;
        publisherParam = param;
        this.threadPool = threadPool;
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

//    public Observable<T> sync() {
//        sync = true;
//        return this;
//    }

    public <TO> SubscriptionBuilder<TO> transform(final Transformer<T, TO> transformer) {
        if (this.transformer != null) {
            throw new IllegalStateException("Only one transformer allowed");
        }
        this.transformer = (Transformer<T, Object>) transformer;
        return (SubscriptionBuilder<TO>) this;
    }

    public SubscriptionBuilder<T> onError(ErrorObserver errorObserver) {
        if (this.errorObserver != null) {
            throw new IllegalStateException("Only one errorObserver allowed");
        }
        this.errorObserver = errorObserver;
        return this;
    }

    public SubscriptionBuilder<T> on(Scheduler scheduler) {
        if (this.scheduler != null) {
            throw new IllegalStateException("Only one scheduler allowed");
        }
        this.scheduler = scheduler;
        return this;
    }

    public Subscription subscribe(final Observer<T> observer) {
        WeakObserver<T> weakObserver = null;
        if (weak) {
            weakObserver = new WeakObserver<>(observer);
            this.observer = weakObserver;
        } else {
            this.observer = observer;
        }
        SubscriptionImpl subscription = new SubscriptionImpl(publisher, publisherParam, observer);
        if(weakObserver!= null) {
            weakObserver.setSubscription(subscription);
        }

        // TODO FIXME when an observer subscribes twice, it currently won't be added, but we return a new subscription
        if (transformer == null && scheduler == null) {
            publisher.subscribe(observer, publisherParam);
        } else {
            publisher.subscribe(new ActionObserver(subscription), publisherParam);
        }
        return subscription;
    }

    class ActionObserver implements Observer<T> {
        private final SubscriptionImpl subscription;
        private SchedulerRunOnError schedulerRunOnError;
        private SchedulerRunOnChange schedulerRunOnChange;

        public ActionObserver(SubscriptionImpl subscription) {
            this.subscription = subscription;
            if (scheduler != null) {
                schedulerRunOnChange = new SchedulerRunOnChange();
                if (errorObserver != null) {
                    schedulerRunOnError = new SchedulerRunOnError();
                }
            }
        }

        @Override
        public void onChange(final T data) {
            if (transformer != null) {
                transformAndContinue(data);
            } else {
                callOnChange(data);
            }
        }

        private void transformAndContinue(final T data) {
            threadPool.submit(new Runnable() {
                @Override
                public void run() {
                    if (subscription.isCanceled()) {
                        return;
                    }
                    try {
                        // Type erasure FTW
                        T result = (T) transformer.transform(data);
                        callOnChange(result);
                    } catch (Throwable th) {
                        if (errorObserver != null) {
                            if (!subscription.isCanceled()) {
                                if (scheduler != null) {
                                    scheduler.run(schedulerRunOnError, th);
                                } else {
                                    errorObserver.onError(th);
                                }
                            }
                        } else {
                            throw new RuntimeException("Transformer failed without an ErrorObserver set", th);
                        }
                    }
                }
            });
        }

        void callOnChange(final T result) {
            if (!subscription.isCanceled()) {
                if (scheduler != null) {
                    scheduler.run(schedulerRunOnChange, result);
                } else {
                    observer.onChange(result);
                }
            }
        }

        class SchedulerRunOnChange implements RunWithParam<T> {
            @Override
            public void run(T data) {
                if (!subscription.isCanceled()) {
                    observer.onChange(data);
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
