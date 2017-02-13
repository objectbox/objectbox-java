package io.objectbox.query;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import io.objectbox.Box;
import io.objectbox.BoxStore;
import io.objectbox.ObjectClassObserver;
import io.objectbox.Property;
import io.objectbox.annotation.apihint.Beta;
import io.objectbox.internal.CallWithHandle;

/**
 * A repeatable query returning entities.
 *
 * @param <T> The entity class the query will return results for.
 * @author Markus
 * @see QueryBuilder
 */
@Beta
public class Query<T> {

    private ObjectClassObserver objectClassObserver;

    private static native long nativeDestroy(long handle);

    private native static Object nativeFindFirst(long handle, long cursorHandle);

    private native static Object nativeFindUnique(long handle, long cursorHandle);

    private native static List nativeFind(long handle, long cursorHandle, long offset, long limit);

    private native static long[] nativeFindKeysUnordered(long handle, long cursorHandle);

    private native static long nativeCount(long handle, long cursorHandle);

    private native static long nativeSum(long handle, long cursorHandle, int propertyId);

    private native static double nativeSumDouble(long handle, long cursorHandle, int propertyId);

    private native static long nativeMax(long handle, long cursorHandle, int propertyId);

    private native static double nativeMaxDouble(long handle, long cursorHandle, int propertyId);

    private native static long nativeMin(long handle, long cursorHandle, int propertyId);

    private native static double nativeMinDouble(long handle, long cursorHandle, int propertyId);

    private native static double nativeAvg(long handle, long cursorHandle, int propertyId);

    private native static long nativeRemove(long handle, long cursorHandle);

    private native static void nativeSetParameter(long handle, int propertyId, String parameterAlias, String value);

    private native static void nativeSetParameter(long handle, int propertyId, String parameterAlias, long value);

    private native static void nativeSetParameters(long handle, int propertyId, String parameterAlias, long value1,
                                                   long value2);

    private native static void nativeSetParameter(long handle, int propertyId, String parameterAlias, double value);

    private native static void nativeSetParameters(long handle, int propertyId, String parameterAlias, double value1,
                                                   double value2);

    private final Box<T> box;
    private final boolean hasOrder;
    private long handle;
    private Set<QueryObserver<T>> observers = new CopyOnWriteArraySet();

    Query(Box<T> box, long queryHandle, boolean hasOrder) {
        this.box = box;
        handle = queryHandle;
        this.hasOrder = hasOrder;
    }

    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

    public synchronized void close() {
        if (handle != 0) {
            nativeDestroy(handle);
            handle = 0;
        }
    }

    public T findFirst() {
        return box.internalCallWithReaderHandle(new CallWithHandle<T>() {
            @Override
            public T call(long cursorHandle) {
                return (T) nativeFindFirst(handle, cursorHandle);
            }
        });
    }

    public T findUnique() {
        return box.internalCallWithReaderHandle(new CallWithHandle<T>() {
            @Override
            public T call(long cursorHandle) {
                return (T) nativeFindUnique(handle, cursorHandle);
            }
        });
    }

    public List<T> find() {
        return box.internalCallWithReaderHandle(new CallWithHandle<List<T>>() {
            @Override
            public List<T> call(long cursorHandle) {
                return nativeFind(handle, cursorHandle, 0, 0);
            }
        });
    }

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

    public LazyList<T> findLazy() {
        return new LazyList<>(box, findIds(), false);
    }

    public LazyList<T> findLazyCached() {
        return new LazyList<>(box, findIds(), true);
    }

    public long count() {
        return box.internalCallWithReaderHandle(new CallWithHandle<Long>() {
            @Override
            public Long call(long cursorHandle) {
                return nativeCount(handle, cursorHandle);
            }
        });
    }

    public long sum(final Property property) {
        return box.internalCallWithReaderHandle(new CallWithHandle<Long>() {
            @Override
            public Long call(long cursorHandle) {
                return nativeSum(handle, cursorHandle, property.getId());
            }
        });
    }

    public double sumDouble(final Property property) {
        return box.internalCallWithReaderHandle(new CallWithHandle<Double>() {
            @Override
            public Double call(long cursorHandle) {
                return nativeSumDouble(handle, cursorHandle, property.getId());
            }
        });
    }

    public long max(final Property property) {
        return box.internalCallWithReaderHandle(new CallWithHandle<Long>() {
            @Override
            public Long call(long cursorHandle) {
                return nativeMax(handle, cursorHandle, property.getId());
            }
        });
    }

    public double maxDouble(final Property property) {
        return box.internalCallWithReaderHandle(new CallWithHandle<Double>() {
            @Override
            public Double call(long cursorHandle) {
                return nativeMaxDouble(handle, cursorHandle, property.getId());
            }
        });
    }

    public long min(final Property property) {
        return box.internalCallWithReaderHandle(new CallWithHandle<Long>() {
            @Override
            public Long call(long cursorHandle) {
                return nativeMin(handle, cursorHandle, property.getId());
            }
        });
    }

    public double minDouble(final Property property) {
        return box.internalCallWithReaderHandle(new CallWithHandle<Double>() {
            @Override
            public Double call(long cursorHandle) {
                return nativeMinDouble(handle, cursorHandle, property.getId());
            }
        });
    }

    public double avg(final Property property) {
        return box.internalCallWithReaderHandle(new CallWithHandle<Double>() {
            @Override
            public Double call(long cursorHandle) {
                return nativeAvg(handle, cursorHandle, property.getId());
            }
        });
    }

    public Query<T> setParameter(Property property, String value) {
        nativeSetParameter(handle, property.getId(), null, value);
        return this;
    }

    public Query<T> setParameter(Property property, long value) {
        nativeSetParameter(handle, property.getId(), null, value);
        return this;
    }

    public Query<T> setParameter(Property property, double value) {
        nativeSetParameter(handle, property.getId(), null, value);
        return this;
    }

    /**
     * @throws NullPointerException if given date is null
     */
    public Query<T> setParameter(Property property, Date value) {
        return setParameter(property, value.getTime());
    }

    public Query<T> setParameter(Property property, boolean value) {
        return setParameter(property, value ? 1 : 0);
    }

    public Query<T> setParameters(Property property, long value1, long value2) {
        nativeSetParameters(handle, property.getId(), null, value1, value2);
        return this;
    }

    public Query<T> setParameters(Property property, double value1, double value2) {
        nativeSetParameters(handle, property.getId(), null, value1, value2);
        return this;
    }

    public long remove() {
        return box.internalCallWithWriterHandle(new CallWithHandle<Long>() {
            @Override
            public Long call(long cursorHandle) {
                return nativeRemove(handle, cursorHandle);
            }
        });
    }

    public synchronized void subscribe(QueryObserver<T> observer) {
        final BoxStore store = box.getStore();
        if (objectClassObserver == null) {
            objectClassObserver = new ObjectClassObserver() {
                @Override
                public void onChanges(Class objectClass) {
                    store.internalScheduleThread(new Runnable() {
                        @Override
                        public void run() {
                            List<T> result = find();
                            for (QueryObserver<T> observer : observers) {
                                observer.onQueryChanges(result);
                            }
                        }
                    });
                }
            };
        }
        if (observers.isEmpty()) {
            store.subscribeWeak(objectClassObserver, box.getEntityClass());
        }
        observers.add(observer);
    }

    public synchronized void unsubscribe(QueryObserver<T> observer) {
        observers.remove(observer);
        if (observers.isEmpty() && objectClassObserver != null) {
            box.getStore().unsubscribe(objectClassObserver);
        }
    }

}
