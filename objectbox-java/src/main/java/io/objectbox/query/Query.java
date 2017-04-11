package io.objectbox.query;

import java.util.Date;
import java.util.List;

import io.objectbox.Box;
import io.objectbox.Property;
import io.objectbox.annotation.apihint.Beta;
import io.objectbox.internal.CallWithHandle;
import io.objectbox.reactive.DataObserver;
import io.objectbox.reactive.SubscriptionBuilder;

/**
 * A repeatable query returning entities.
 *
 * @param <T> The entity class the query will return results for.
 * @author Markus
 * @see QueryBuilder
 */
@Beta
public class Query<T> {

    static native long nativeDestroy(long handle);

    native static Object nativeFindFirst(long handle, long cursorHandle);

    native static Object nativeFindUnique(long handle, long cursorHandle);

    native static List nativeFind(long handle, long cursorHandle, long offset, long limit);

    native static long[] nativeFindKeysUnordered(long handle, long cursorHandle);

    native static long nativeCount(long handle, long cursorHandle);

    native static long nativeSum(long handle, long cursorHandle, int propertyId);

    native static double nativeSumDouble(long handle, long cursorHandle, int propertyId);

    native static long nativeMax(long handle, long cursorHandle, int propertyId);

    native static double nativeMaxDouble(long handle, long cursorHandle, int propertyId);

    native static long nativeMin(long handle, long cursorHandle, int propertyId);

    native static double nativeMinDouble(long handle, long cursorHandle, int propertyId);

    native static double nativeAvg(long handle, long cursorHandle, int propertyId);

    native static long nativeRemove(long handle, long cursorHandle);

    native static void nativeSetParameter(long handle, int propertyId, String parameterAlias, String value);

    native static void nativeSetParameter(long handle, int propertyId, String parameterAlias, long value);

    native static void nativeSetParameters(long handle, int propertyId, String parameterAlias, long value1,
                                           long value2);

    native static void nativeSetParameter(long handle, int propertyId, String parameterAlias, double value);

    native static void nativeSetParameters(long handle, int propertyId, String parameterAlias, double value1,
                                                   double value2);

    private final Box<T> box;
    private final boolean hasOrder;
    long handle;
    private final QueryPublisher<T> publisher;

    Query(Box<T> box, long queryHandle, boolean hasOrder) {
        this.box = box;
        handle = queryHandle;
        this.hasOrder = hasOrder;
        publisher = new QueryPublisher<T>(this, box);
    }

    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

    /**
     * If possible, try to close the query once you are done with it to reclaim resources immediately.
     */
    public synchronized void close() {
        if (handle != 0) {
            nativeDestroy(handle);
            handle = 0;
        }
    }

    /**
     * Find the first Object matching the query.
     */
    public T findFirst() {
        return box.internalCallWithReaderHandle(new CallWithHandle<T>() {
            @Override
            public T call(long cursorHandle) {
                return (T) nativeFindFirst(handle, cursorHandle);
            }
        });
    }

    /**
     * Find the unique Object matching the query.
     *
     * @throws io.objectbox.exception.DbException if result was not unique
     */
    public T findUnique() {
        return box.internalCallWithReaderHandle(new CallWithHandle<T>() {
            @Override
            public T call(long cursorHandle) {
                return (T) nativeFindUnique(handle, cursorHandle);
            }
        });
    }

    /**
     * Find all Objects matching the query.
     */
    public List<T> find() {
        return box.internalCallWithReaderHandle(new CallWithHandle<List<T>>() {
            @Override
            public List<T> call(long cursorHandle) {
                return nativeFind(handle, cursorHandle, 0, 0);
            }
        });
    }

    /**
     * Find all Objects matching the query between the given offset and limit. This helps with pagination.
     */
    public List<T> find(final long offset, final long limit) {
        return box.internalCallWithReaderHandle(new CallWithHandle<List<T>>() {
            @Override
            public List<T> call(long cursorHandle) {
                return nativeFind(handle, cursorHandle, offset, limit);
            }
        });
    }

    /**
     * Very efficient way to get just the IDs without creating any objects. IDs can later be used to lookup objects
     * (lookups by ID are also very efficient in ObjectBox).
     */
    public long[] findIds() {
        if (hasOrder) {
            throw new UnsupportedOperationException("This method is currently only available for unordered queries");
        }
        return box.internalCallWithReaderHandle(new CallWithHandle<long[]>() {
            @Override
            public long[] call(long cursorHandle) {
                return nativeFindKeysUnordered(handle, cursorHandle);
            }
        });
    }

    /**
     * Find all Objects matching the query without actually loading the Objects. See @{@link LazyList} for details.
     */
    public LazyList<T> findLazy() {
        return new LazyList<>(box, findIds(), false);
    }

    /**
     * Emits query results one by one to the given consumer.
     * It "streams" each object from the database to the consumer, which is very memory efficient.
     * Because this is run in a read transaction, the consumer gets a consistent view on the data.
     * Like {@link #findLazy()}, this method can be used for a high amount of data.
     * <p>
     * Note: because the consumer is called within a read transaction it may not write to the database.
     */
    public void forEach(final QueryConsumer<T> consumer) {
        box.getStore().runInReadTx(new Runnable() {
            @Override
            public void run() {
                LazyList<T> lazyList = findLazy();
                int size = lazyList.size();
                for (int i = 0; i < size; i++) {
                    T data = lazyList.get(i);
                    if (data == null) {
                        throw new IllegalStateException("Internal error: data object was null");
                    }
                    consumer.accept(data);
                }
            }
        });
    }

    /**
     * Find all Objects matching the query without actually loading the Objects. See @{@link LazyList} for details.
     */
    public LazyList<T> findLazyCached() {
        return new LazyList<>(box, findIds(), true);
    }

    /** Returns the count of Objects matching the query. */
    public long count() {
        return box.internalCallWithReaderHandle(new CallWithHandle<Long>() {
            @Override
            public Long call(long cursorHandle) {
                return nativeCount(handle, cursorHandle);
            }
        });
    }

    /** Sums up all values for the given property over all Objects matching the query. */
    public long sum(final Property property) {
        return box.internalCallWithReaderHandle(new CallWithHandle<Long>() {
            @Override
            public Long call(long cursorHandle) {
                return nativeSum(handle, cursorHandle, property.getId());
            }
        });
    }

    /** Sums up all values for the given property over all Objects matching the query. */
    public double sumDouble(final Property property) {
        return box.internalCallWithReaderHandle(new CallWithHandle<Double>() {
            @Override
            public Double call(long cursorHandle) {
                return nativeSumDouble(handle, cursorHandle, property.getId());
            }
        });
    }

    /** Finds the maximum value for the given property over all Objects matching the query. */
    public long max(final Property property) {
        return box.internalCallWithReaderHandle(new CallWithHandle<Long>() {
            @Override
            public Long call(long cursorHandle) {
                return nativeMax(handle, cursorHandle, property.getId());
            }
        });
    }

    /** Finds the maximum value for the given property over all Objects matching the query. */
    public double maxDouble(final Property property) {
        return box.internalCallWithReaderHandle(new CallWithHandle<Double>() {
            @Override
            public Double call(long cursorHandle) {
                return nativeMaxDouble(handle, cursorHandle, property.getId());
            }
        });
    }

    /** Finds the minimum value for the given property over all Objects matching the query. */
    public long min(final Property property) {
        return box.internalCallWithReaderHandle(new CallWithHandle<Long>() {
            @Override
            public Long call(long cursorHandle) {
                return nativeMin(handle, cursorHandle, property.getId());
            }
        });
    }

    /** Finds the minimum value for the given property over all Objects matching the query. */
    public double minDouble(final Property property) {
        return box.internalCallWithReaderHandle(new CallWithHandle<Double>() {
            @Override
            public Double call(long cursorHandle) {
                return nativeMinDouble(handle, cursorHandle, property.getId());
            }
        });
    }

    /** Calculates the average of all values for the given property over all Objects matching the query. */
    public double avg(final Property property) {
        return box.internalCallWithReaderHandle(new CallWithHandle<Double>() {
            @Override
            public Double call(long cursorHandle) {
                return nativeAvg(handle, cursorHandle, property.getId());
            }
        });
    }


    /**
     * Sets a parameter previously given to the {@link QueryBuilder} to a new value.
     */
    public Query<T> setParameter(Property property, String value) {
        nativeSetParameter(handle, property.getId(), null, value);
        return this;
    }

    /**
     * Sets a parameter previously given to the {@link QueryBuilder} to a new value.
     */
    public Query<T> setParameter(Property property, long value) {
        nativeSetParameter(handle, property.getId(), null, value);
        return this;
    }

    /**
     * Sets a parameter previously given to the {@link QueryBuilder} to a new value.
     */
    public Query<T> setParameter(Property property, double value) {
        nativeSetParameter(handle, property.getId(), null, value);
        return this;
    }

    /**
     * Sets a parameter previously given to the {@link QueryBuilder} to a new value.
     *
     * @throws NullPointerException if given date is null
     */
    public Query<T> setParameter(Property property, Date value) {
        return setParameter(property, value.getTime());
    }

    /**
     * Sets a parameter previously given to the {@link QueryBuilder} to a new value.
     */
    public Query<T> setParameter(Property property, boolean value) {
        return setParameter(property, value ? 1 : 0);
    }

    /**
     * Sets a parameter previously given to the {@link QueryBuilder} to new values.
     */
    public Query<T> setParameters(Property property, long value1, long value2) {
        nativeSetParameters(handle, property.getId(), null, value1, value2);
        return this;
    }

    /**
     * Sets a parameter previously given to the {@link QueryBuilder} to new values.
     */
    public Query<T> setParameters(Property property, double value1, double value2) {
        nativeSetParameters(handle, property.getId(), null, value1, value2);
        return this;
    }

    /**
     * Removes (deletes) all Objects matching the query
     *
     * @return count of removed Objects
     */
    public long remove() {
        return box.internalCallWithWriterHandle(new CallWithHandle<Long>() {
            @Override
            public Long call(long cursorHandle) {
                return nativeRemove(handle, cursorHandle);
            }
        });
    }

    /**
     * A {@link io.objectbox.reactive.DataObserver} can be subscribed to data changes using the returned builder.
     * The observer is supplied via {@link SubscriptionBuilder#observer(DataObserver)} and will be notified once
     * the query results have (potentially) changed.
     * <p>
     * With subscribing, the observer will immediately get current query results.
     * The query is run for the subscribing observer.
     * <p>
     * Threading notes:
     * Query observers are notified from a thread pooled. Observers may be notified in parallel.
     * The notification order is the same as the subscription order, although this may not always be guaranteed in
     * the future.
     * <p>
     * Stale observers: you must hold on to the Query or {@link io.objectbox.reactive.DataSubscription} objects to keep
     * your {@link DataObserver}s active. If this Query is not referenced anymore
     * (along with its {@link io.objectbox.reactive.DataSubscription}s, which hold a reference to the Query internally),
     * it may be GCed and observers may become stale (won't receive anymore data).
     */
    public SubscriptionBuilder<List<T>> subscribe() {
        return new SubscriptionBuilder<>(publisher, null, box.getStore().internalThreadPool());
    }

    /**
     * Publishes the current data to all subscribed @{@link DataObserver}s.
     * This is useful triggering observers when new parameters have been set.
     * Note, that setParameter methods will NOT be propagated to observers.
     */
    public void publish() {
        publisher.publish();
    }

}
