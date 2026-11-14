/*
 * Copyright 2026 ObjectBox Ltd. <https://objectbox.io>
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

package io.objectbox.query;

import java.io.Closeable;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import io.objectbox.Box;
import io.objectbox.BoxStore;
import io.objectbox.InternalAccess;
import io.objectbox.Property;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.HnswIndex;
import io.objectbox.annotation.VectorSearchLevel;
import io.objectbox.exception.NonUniqueResultException;
import io.objectbox.reactive.DataObserver;
import io.objectbox.reactive.DataSubscriptionList;
import io.objectbox.reactive.SubscriptionBuilder;
import io.objectbox.relation.RelationInfo;
import io.objectbox.relation.ToOne;

/**
 * A repeatable Query returning the latest matching objects.
 * <p>
 * Use {@link #find()} or related methods to fetch the latest results from the {@link BoxStore}.
 * <p>
 * Use {@link #property(Property)} to only return values or an aggregate of a single Property.
 * <p>
 * Make sure to {@link #close()} this query once done with it to reclaim resources immediately.
 * <p>
 * See the <a href="https://docs.objectbox.io/queries">Queries documentation</a> for details.
 *
 * @param <T> Entity class for which results are returned.
 */
@SuppressWarnings({"SameParameterValue", "UnusedReturnValue", "WeakerAccess"})
public class Query<T> implements Closeable {

    native void nativeDestroy(long handle);

    /** Clones the native query, incl. conditions and parameters, and returns a handle to the clone. */
    native long nativeClone(long handle);

    native Object nativeFindFirst(long handle, long cursorHandle);

    native Object nativeFindUnique(long handle, long cursorHandle);

    native List<T> nativeFind(long handle, long cursorHandle, long offset, long limit) throws Exception;

    native long nativeFindFirstId(long handle, long cursorHandle);

    native long nativeFindUniqueId(long handle, long cursorHandle);

    native long[] nativeFindIds(long handle, long cursorHandle, long offset, long limit);

    native List<ObjectWithScore<T>> nativeFindWithScores(long handle, long cursorHandle, long offset, long limit);

    native List<IdWithScore> nativeFindIdsWithScores(long handle, long cursorHandle, long offset, long limit);

    native long nativeCount(long handle, long cursorHandle);

    native long nativeRemove(long handle, long cursorHandle);

    native String nativeToString(long handle);

    native String nativeDescribeParameters(long handle);

    native void nativeSetParameter(long handle, int entityId, int propertyId, @Nullable String parameterAlias,
                                   String value);

    private native void nativeSetParameters(long handle, int entityId, int propertyId, @Nullable String parameterAlias,
                                            String value, String value2);

    native void nativeSetParameter(long handle, int entityId, int propertyId, @Nullable String parameterAlias,
                                   long value);

    native void nativeSetParameters(long handle, int entityId, int propertyId, @Nullable String parameterAlias,
                                    int[] values);

    native void nativeSetParameters(long handle, int entityId, int propertyId, @Nullable String parameterAlias,
                                    long[] values);

    native void nativeSetParameters(long handle, int entityId, int propertyId, @Nullable String parameterAlias,
                                    long value1, long value2);

    native void nativeSetParameter(long handle, int entityId, int propertyId, @Nullable String parameterAlias,
                                   double value);

    native void nativeSetParameters(long handle, int entityId, int propertyId, @Nullable String parameterAlias,
                                    double value1, double value2);

    native void nativeSetParameters(long handle, int entityId, int propertyId, @Nullable String parameterAlias,
                                    String[] values);

    native void nativeSetParameter(long handle, int entityId, int propertyId, @Nullable String parameterAlias,
                                   byte[] value);

    native void nativeSetParameter(long handle, int entityId, int propertyId, @Nullable String parameterAlias,
                                    float[] values);

    native void nativeSetParameterVectorSearchLevel(long handle, int entityId, int propertyId,
                                                    @Nullable String parameterAlias, int level);

    native void nativeSetParameterVectorOversampleFactor(long handle, int entityId, int propertyId,
                                                         @Nullable String parameterAlias, float value);

    native void nativeSetParameterVectorQuantizationOversample(long handle, int entityId, int propertyId,
                                                               @Nullable String parameterAlias, float value);

    native void nativeSetParameterVectorRescore(long handle, int entityId, int propertyId,
                                                @Nullable String parameterAlias, boolean value);

    native void nativeSetParameterVectorRescoreFactor(long handle, int entityId, int propertyId,
                                                      @Nullable String parameterAlias, float value);

    native void nativeSetParameterVectorBruteForceMaxCandidates(long handle, int entityId, int propertyId,
                                                                @Nullable String parameterAlias, int value);

    native void nativeSetParameterVectorFilteredSearchMaxNodes(long handle, int entityId, int propertyId,
                                                               @Nullable String parameterAlias, int value);

    final Box<T> box;
    private final BoxStore store;
    private final QueryPublisher<T> publisher;
    @Nullable private final List<EagerRelation<T, ?>> eagerRelations;
    @Nullable private final QueryFilter<T> filter;
    @Nullable private final Comparator<T> comparator;
    private final int queryAttempts;
    private static final int INITIAL_RETRY_BACK_OFF_IN_MS = 10;

    // volatile so checkOpen() is more up-to-date (no need for synchronized; it's a race anyway)
    volatile long handle;

    Query(Box<T> box, long queryHandle, @Nullable List<EagerRelation<T, ?>> eagerRelations, @Nullable QueryFilter<T> filter,
          @Nullable Comparator<T> comparator) {
        this.box = box;
        store = box.getStore();
        queryAttempts = store.internalQueryAttempts();
        handle = queryHandle;
        publisher = new QueryPublisher<>(this, box);
        this.eagerRelations = eagerRelations;
        this.filter = filter;
        this.comparator = comparator;
    }

    /**
     * Creates a copy of the {@code originalQuery}, but pointing to a different native query using {@code handle}.
     */
    // Note: not using recommended copy constructor (just passing this) as handle needs to change.
    private Query(Query<T> originalQuery, long handle) {
        this(
                originalQuery.box,
                handle,
                originalQuery.eagerRelations,
                originalQuery.filter,
                originalQuery.comparator
        );
    }

    /**
     * Explicitly call {@link #close()} instead to avoid expensive finalization.
     */
    @SuppressWarnings("deprecation") // finalize()
    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

    /**
     * Closes this query and frees used resources.
     * <p>
     * If possible, call this always once done with this. Otherwise, will be called once this is finalized (e.g. garbage
     * collected).
     * <p>
     * Calling any other methods of this afterwards will throw an {@link IllegalStateException}.
     */
    public synchronized void close() {
        publisher.stopAndAwait();  // Ensure it is done so that the query is not used anymore
        if (handle != 0) {
            // Closeable recommendation: mark as "closed" before nativeDestroy could throw.
            long handleCopy = handle;
            handle = 0;
            nativeDestroy(handleCopy);
        }
    }

    /**
     * Creates a copy of this for use in another thread.
     * <p>
     * Clones the native query, keeping any previously set parameters.
     * <p>
     * Closing the original query does not close the copy. {@link #close()} the copy once finished using it.
     * <p>
     * Note: a set {@link QueryBuilder#filter(QueryFilter) filter} or {@link QueryBuilder#sort(Comparator) sort}
     * order <b>must be thread safe</b>.
     */
    // Note: not overriding clone() to avoid confusion with Java's cloning mechanism.
    public Query<T> copy() {
        long cloneHandle = nativeClone(handle);
        return new Query<>(this, cloneHandle);
    }

    /** To be called inside a read TX */
    long cursorHandle() {
        return InternalAccess.getActiveTxCursorHandle(box);
    }

    /**
     * Finds the first object matching this query.
     * <p>
     * Note: if no {@link QueryBuilder#order} conditions are present, which object is the first one might be arbitrary
     * (sometimes the one with the lowest ID, but never guaranteed to be).
     *
     * @return The first object if there are matches. {@code null} if no object matches.
     */
    @Nullable
    public T findFirst() {
        ensureNoFilterNoComparator();
        return callInReadTx(() -> {
            @SuppressWarnings("unchecked")
            T entity = (T) nativeFindFirst(handle, cursorHandle());
            resolveEagerRelation(entity);
            return entity;
        });
    }

    private void ensureNoFilterNoComparator() {
        ensureNoFilter();
        ensureNoComparator();
    }

    private void ensureNoFilter() {
        if (filter != null) {
            throw new UnsupportedOperationException("Does not work with a filter. " +
                    "Only find() and forEach() support filters.");
        }
    }

    private void ensureNoComparator() {
        if (comparator != null) {
            throw new UnsupportedOperationException("Does not work with a sorting comparator. " +
                    "Only find() supports sorting with a comparator.");
        }
    }

    /**
     * Finds the only object matching this query.
     *
     * @return The object if a single object matches. {@code null} if no object matches. Throws
     * {@link NonUniqueResultException} if there are multiple objects matching the query.
     */
    @Nullable
    public T findUnique() {
        ensureNoFilter();  // Comparator is fine: does not make any difference for a unique result
        return callInReadTx(() -> {
            @SuppressWarnings("unchecked")
            T entity = (T) nativeFindUnique(handle, cursorHandle());
            resolveEagerRelation(entity);
            return entity;
        });
    }

    /**
     * Finds objects matching the query.
     * <p>
     * Note: if no {@link QueryBuilder#order} conditions are present, the order is arbitrary (sometimes ordered by ID,
     * but never guaranteed to).
     *
     * @return A list of matching objects. An empty list if no object matches.
     */
    @Nonnull
    public List<T> find() {
        return callInReadTx(() -> {
            List<T> entities = nativeFind(Query.this.handle, cursorHandle(), 0, 0);
            if (filter != null) {
                Iterator<T> iterator = entities.iterator();
                while (iterator.hasNext()) {
                    T entity = iterator.next();
                    if (!filter.keep(entity)) {
                        iterator.remove();
                    }
                }
            }
            resolveEagerRelations(entities);
            if (comparator != null) {
                Collections.sort(entities, comparator);
            }
            return entities;
        });
    }

    /**
     * Like {@link #find()}, but can skip and limit results.
     * <p>
     * Use to get a slice of the whole result, e.g. for "result paging".
     *
     * @param offset If greater than 0, skips this many results.
     * @param limit If greater than 0, returns at most this many results.
     */
    @Nonnull
    public List<T> find(final long offset, final long limit) {
        ensureNoFilterNoComparator();
        return callInReadTx(() -> {
            List<T> entities = nativeFind(handle, cursorHandle(), offset, limit);
            resolveEagerRelations(entities);
            return entities;
        });
    }

    /**
     * Like {@link #findFirst()}, but returns just the ID of the object.
     * <p>
     * This is more efficient as no object is created.
     * <p>
     * Ignores any {@link QueryBuilder#filter(QueryFilter) query filter}.
     *
     * @return The ID of the first matching object. {@code 0} if no object matches.
     */
    public long findFirstId() {
        checkOpen();
        return box.internalCallWithReaderHandle(cursorHandle -> nativeFindFirstId(handle, cursorHandle));
    }

    /**
     * Like {@link #findUnique()}, but returns just the ID of the object.
     * <p>
     * This is more efficient as no object is created.
     * <p>
     * Ignores any {@link QueryBuilder#filter(QueryFilter) query filter}.
     *
     * @return The ID of the object, if a single object matches. {@code 0} if no object matches. Throws
     * {@link NonUniqueResultException} if there are multiple objects matching the query.
     */
    public long findUniqueId() {
        checkOpen();
        return box.internalCallWithReaderHandle(cursorHandle -> nativeFindUniqueId(handle, cursorHandle));
    }

    /**
     * Like {@link #find()}, but returns just the IDs of the objects.
     * <p>
     * IDs can later be used to {@link Box#get} objects.
     * <p>
     * This is very efficient as no objects are created.
     * <p>
     * Note: a filter set with {@link QueryBuilder#filter(QueryFilter)} will be silently ignored!
     *
     * @return An array of IDs of matching objects. An empty array if no objects match.
     */
    @Nonnull
    public long[] findIds() {
        return findIds(0, 0);
    }

    /**
     * Like {@link #findIds()}, but can skip and limit results.
     * <p>
     * Use to get a slice of the whole result, e.g. for "result paging".
     * <p>
     * Note: a filter set with {@link QueryBuilder#filter(QueryFilter)} will be silently ignored!
     *
     * @param offset If greater than 0, skips this many results.
     * @param limit If greater than 0, returns at most this many results.
     */
    @Nonnull
    public long[] findIds(final long offset, final long limit) {
        checkOpen();
        return box.internalCallWithReaderHandle(cursorHandle -> nativeFindIds(handle, cursorHandle, offset, limit));
    }

    /**
     * Like {@link #findIds()}, but wraps the Object IDs in an unmodifiable {@link LazyList}
     * so Objects can be retrieved on demand. The LazyList does not cache retrieved Objects, so only basic
     * {@link List} operations like getting or iterating list items are supported. See {@link LazyList} for details.
     */
    @Nonnull
    public LazyList<T> findLazy() {
        ensureNoFilterNoComparator();
        return new LazyList<>(box, findIds(), false);
    }

    /**
     * Like {@link #findIds()}, but wraps the Object IDs in an unmodifiable, caching {@link LazyList}
     * so Objects can be retrieved on demand. The LazyList caches retrieved Objects supporting almost
     * all {@link List} operations, at the expense of used memory. See {@link LazyList} for details.
     */
    @Nonnull
    public LazyList<T> findLazyCached() {
        ensureNoFilterNoComparator();
        return new LazyList<>(box, findIds(), true);
    }

    /**
     * Like {@link #findIdsWithScores()}, but can skip and limit results.
     * <p>
     * Use to get a slice of the whole result, e.g. for "result paging".
     *
     * @param offset If greater than 0, skips this many results.
     * @param limit If greater than 0, returns at most this many results.
     */
    @Nonnull
    public List<IdWithScore> findIdsWithScores(final long offset, final long limit) {
        checkOpen();
        return box.internalCallWithReaderHandle(cursorHandle -> nativeFindIdsWithScores(handle, cursorHandle, offset, limit));
    }

    /**
     * Finds IDs of objects matching the query associated to their query score (e.g. distance in NN search).
     * <p>
     * This only works on objects with a property with an {@link HnswIndex}.
     *
     * @return A list of {@link IdWithScore} that wraps IDs of matching objects and their score, sorted by score in
     * ascending order.
     */
    @Nonnull
    public List<IdWithScore> findIdsWithScores() {
        return findIdsWithScores(0, 0);
    }

    /**
     * Like {@link #findWithScores()}, but can skip and limit results.
     * <p>
     * Use to get a slice of the whole result, e.g. for "result paging".
     *
     * @param offset If greater than 0, skips this many results.
     * @param limit If greater than 0, returns at most this many results.
     */
    @Nonnull
    public List<ObjectWithScore<T>> findWithScores(final long offset, final long limit) {
        ensureNoFilterNoComparator();
        return callInReadTx(() -> {
            List<ObjectWithScore<T>> results = nativeFindWithScores(handle, cursorHandle(), offset, limit);
            if (eagerRelations != null) {
                for (int i = 0; i < results.size(); i++) {
                    resolveEagerRelationForNonNullEagerRelations(results.get(i).get(), i);
                }
            }
            return results;
        });
    }

    /**
     * Finds objects matching the query associated to their query score (e.g. distance in NN search).
     * <p>
     * This only works on objects with a property with an {@link HnswIndex}.
     *
     * @return A list of {@link ObjectWithScore} that wraps matching objects and their score, sorted by score in
     * ascending order.
     */
    @Nonnull
    public List<ObjectWithScore<T>> findWithScores() {
        return findWithScores(0, 0);
    }

    /**
     * Creates a {@link PropertyQuery} for the given property.
     * <p>
     * A {@link PropertyQuery} uses the same conditions as this Query object,
     * but returns only the value(s) of a single property (not an entity objects).
     *
     * @param property the property for which to return values
     */
    public PropertyQuery property(Property<T> property) {
        return new PropertyQuery(this, property);
    }

    <R> R callInReadTx(Callable<R> callable) {
        checkOpen();
        return store.callInReadTxWithRetry(callable, queryAttempts, INITIAL_RETRY_BACK_OFF_IN_MS, true);
    }

    /**
     * Emits query results one by one to the given consumer (synchronously).
     * Once this method returns, the consumer will have received all result object).
     * It "streams" each object from the database to the consumer, which is very memory efficient.
     * Because this is run in a read transaction, the consumer gets a consistent view on the data.
     * Like {@link #findLazy()}, this method can be used for a high amount of data.
     * <p>
     * Note: because the consumer is called within a read transaction it may not write to the database.
     */
    public void forEach(final QueryConsumer<T> consumer) {
        ensureNoComparator();
        checkOpen(); // findIds also checks, but throw early outside of transaction.
        box.getStore().runInReadTx(() -> {
            LazyList<T> lazyList = new LazyList<>(box, findIds(), false);
            int size = lazyList.size();
            for (int i = 0; i < size; i++) {
                T entity = lazyList.get(i);
                if (entity == null) {
                    throw new IllegalStateException("Internal error: data object was null");
                }
                if (filter != null) {
                    if (!filter.keep(entity)) {
                        continue;
                    }
                }
                if (eagerRelations != null) {
                    resolveEagerRelationForNonNullEagerRelations(entity, i);
                }
                try {
                    consumer.accept(entity);
                } catch (BreakForEach breakForEach) {
                    break;
                }
            }
        });
    }

    void resolveEagerRelations(List<T> entities) {
        if (eagerRelations != null) {
            int entityIndex = 0;
            for (T entity : entities) {
                resolveEagerRelationForNonNullEagerRelations(entity, entityIndex);
                entityIndex++;
            }
        }
    }

    /** Note: no null check on eagerRelations! */
    void resolveEagerRelationForNonNullEagerRelations(@Nonnull T entity, int entityIndex) {
        //noinspection ConstantConditions No null check.
        for (EagerRelation<T, ?> eagerRelation : eagerRelations) {
            if (eagerRelation.limit == 0 || entityIndex < eagerRelation.limit) {
                resolveEagerRelation(entity, eagerRelation);
            }
        }
    }

    void resolveEagerRelation(@Nullable T entity) {
        if (eagerRelations != null && entity != null) {
            for (EagerRelation<T, ?> eagerRelation : eagerRelations) {
                resolveEagerRelation(entity, eagerRelation);
            }
        }
    }

    void resolveEagerRelation(@Nonnull T entity, EagerRelation<T, ?> eagerRelation) {
        if (eagerRelations != null) {
            RelationInfo<T, ?> relationInfo = eagerRelation.relationInfo;
            if (relationInfo.toOneGetter != null) {
                ToOne<?> toOne = relationInfo.toOneGetter.getToOne(entity);
                if (toOne != null) {
                    toOne.getTarget();
                }
            } else {
                if (relationInfo.toManyGetter == null) {
                    throw new IllegalStateException("Relation info without relation getter: " + relationInfo);
                }
                List<?> toMany = relationInfo.toManyGetter.getToMany(entity);
                if (toMany != null) {
                    //noinspection ResultOfMethodCallIgnored Triggers fetching target entities.
                    toMany.size();
                }
            }
        }
    }

    /** Returns the count of Objects matching the query. */
    public long count() {
        checkOpen();
        ensureNoFilter();
        return box.internalCallWithReaderHandle(cursorHandle -> nativeCount(handle, cursorHandle));
    }

    /**
     * Sets a parameter previously given to the {@link QueryBuilder} to a new value.
     */
    public Query<T> setParameter(Property<?> property, String value) {
        checkOpen();
        nativeSetParameter(handle, property.getEntityId(), property.getId(), null, value);
        return this;
    }

    /**
     * Changes the parameter of the query condition with the matching {@code alias} to a new {@code value}.
     *
     * @param alias as defined using {@link PropertyQueryCondition#alias(String)}.
     * @param value The new value to use for the query condition.
     */
    public Query<T> setParameter(String alias, String value) {
        checkOpen();
        nativeSetParameter(handle, 0, 0, alias, value);
        return this;
    }

    /**
     * Sets a parameter previously given to the {@link QueryBuilder} to a new value.
     */
    public Query<T> setParameter(Property<?> property, long value) {
        checkOpen();
        nativeSetParameter(handle, property.getEntityId(), property.getId(), null, value);
        return this;
    }

    /**
     * Changes the parameter of the query condition with the matching {@code alias} to a new {@code value}.
     *
     * @param alias as defined using {@link PropertyQueryCondition#alias(String)}.
     * @param value The new value to use for the query condition.
     */
    public Query<T> setParameter(String alias, long value) {
        checkOpen();
        nativeSetParameter(handle, 0, 0, alias, value);
        return this;
    }

    /**
     * Sets a parameter previously given to the {@link QueryBuilder} to a new value.
     */
    public Query<T> setParameter(Property<?> property, double value) {
        checkOpen();
        nativeSetParameter(handle, property.getEntityId(), property.getId(), null, value);
        return this;
    }

    /**
     * Changes the parameter of the query condition with the matching {@code alias} to a new {@code value}.
     *
     * @param alias as defined using {@link PropertyQueryCondition#alias(String)}.
     * @param value The new value to use for the query condition.
     */
    public Query<T> setParameter(String alias, double value) {
        checkOpen();
        nativeSetParameter(handle, 0, 0, alias, value);
        return this;
    }

    /**
     * Sets a parameter previously given to the {@link QueryBuilder} to a new value.
     *
     * @throws NullPointerException if given date is null
     */
    public Query<T> setParameter(Property<?> property, Date value) {
        return setParameter(property, value.getTime());
    }

    /**
     * Changes the parameter of the query condition with the matching {@code alias} to a new {@code value}.
     *
     * @param alias as defined using {@link PropertyQueryCondition#alias(String)}.
     * @param value The new value to use for the query condition.
     * @throws NullPointerException if given date is null
     */
    public Query<T> setParameter(String alias, Date value) {
        return setParameter(alias, value.getTime());
    }

    /**
     * Sets a parameter previously given to the {@link QueryBuilder} to a new value.
     */
    public Query<T> setParameter(Property<?> property, boolean value) {
        return setParameter(property, value ? 1 : 0);
    }

    /**
     * Changes the parameter of the query condition with the matching {@code alias} to a new {@code value}.
     *
     * @param alias as defined using {@link PropertyQueryCondition#alias(String)}.
     * @param value The new value to use for the query condition.
     */
    public Query<T> setParameter(String alias, boolean value) {
        return setParameter(alias, value ? 1 : 0);
    }

    /**
     * Changes the parameter of the query condition for {@code property} to a new {@code value}.
     *
     * @param property Property reference from generated entity underscore class, like {@code Example_.example}.
     * @param value The new {@code int[]} value to use for the query condition.
     */
    public Query<T> setParameter(Property<?> property, int[] value) {
        checkOpen();
        nativeSetParameters(handle, property.getEntityId(), property.getId(), null, value);
        return this;
    }

    /**
     * Changes the parameter of the query condition with the matching {@code alias} to a new {@code value}.
     *
     * @param alias as defined using {@link PropertyQueryCondition#alias(String)}.
     * @param value The new {@code int[]} value to use for the query condition.
     */
    public Query<T> setParameter(String alias, int[] value) {
        checkOpen();
        nativeSetParameters(handle, 0, 0, alias, value);
        return this;
    }

    /**
     * Changes the parameter of the query condition for {@code property} to a new {@code value}.
     *
     * @param property Property reference from generated entity underscore class, like {@code Example_.example}.
     * @param value The new {@code long[]} value to use for the query condition.
     */
    public Query<T> setParameter(Property<?> property, long[] value) {
        checkOpen();
        nativeSetParameters(handle, property.getEntityId(), property.getId(), null, value);
        return this;
    }

    /**
     * Changes the parameter of the query condition with the matching {@code alias} to a new {@code value}.
     *
     * @param alias as defined using {@link PropertyQueryCondition#alias(String)}.
     * @param value The new {@code long[]} value to use for the query condition.
     */
    public Query<T> setParameter(String alias, long[] value) {
        checkOpen();
        nativeSetParameters(handle, 0, 0, alias, value);
        return this;
    }

    /**
     * Changes the parameter of the query condition for {@code property} to a new {@code value}.
     *
     * @param property Property reference from generated entity underscore class, like {@code Example_.example}.
     * @param value The new {@code float[]} value to use for the query condition.
     */
    public Query<T> setParameter(Property<?> property, float[] value) {
        checkOpen();
        nativeSetParameter(handle, property.getEntityId(), property.getId(), null, value);
        return this;
    }

    /**
     * Changes the parameter of the query condition with the matching {@code alias} to a new {@code value}.
     *
     * @param alias as defined using {@link PropertyQueryCondition#alias(String)}.
     * @param value The new {@code float[]} value to use for the query condition.
     */
    public Query<T> setParameter(String alias, float[] value) {
        checkOpen();
        nativeSetParameter(handle, 0, 0, alias, value);
        return this;
    }

    /**
     * Changes the parameter of the query condition for {@code property} to a new {@code value}.
     *
     * @param property Property reference from generated entity underscore class, like {@code Example_.example}.
     * @param value The new {@code String[]} value to use for the query condition.
     */
    public Query<T> setParameter(Property<?> property, String[] value) {
        checkOpen();
        nativeSetParameters(handle, property.getEntityId(), property.getId(), null, value);
        return this;
    }

    /**
     * Changes the parameter of the query condition with the matching {@code alias} to a new {@code value}.
     *
     * @param alias as defined using {@link PropertyQueryCondition#alias(String)}.
     * @param value The new {@code String[]} value to use for the query condition.
     */
    public Query<T> setParameter(String alias, String[] value) {
        checkOpen();
        nativeSetParameters(handle, 0, 0, alias, value);
        return this;
    }

    /**
     * Sets a parameter previously given to the {@link QueryBuilder} to new values.
     */
    public Query<T> setParameters(Property<?> property, long value1, long value2) {
        checkOpen();
        nativeSetParameters(handle, property.getEntityId(), property.getId(), null, value1, value2);
        return this;
    }

    /**
     * Changes the parameters of the query condition with the matching {@code alias} to the new values.
     *
     * @param alias as defined using {@link PropertyQueryCondition#alias(String)}.
     * @param value1 The first value to use for the query condition.
     * @param value2 The second value to use for the query condition.
     */
    public Query<T> setParameters(String alias, long value1, long value2) {
        checkOpen();
        nativeSetParameters(handle, 0, 0, alias, value1, value2);
        return this;
    }

    /**
     * Sets a parameter previously given to the {@link QueryBuilder} to new values.
     */
    public Query<T> setParameters(Property<?> property, double value1, double value2) {
        checkOpen();
        nativeSetParameters(handle, property.getEntityId(), property.getId(), null, value1, value2);
        return this;
    }

    /**
     * Changes the parameters of the query condition with the matching {@code alias} to the new values.
     *
     * @param alias as defined using {@link PropertyQueryCondition#alias(String)}.
     * @param value1 The first value to use for the query condition.
     * @param value2 The second value to use for the query condition.
     */
    public Query<T> setParameters(String alias, double value1, double value2) {
        checkOpen();
        nativeSetParameters(handle, 0, 0, alias, value1, value2);
        return this;
    }

    /**
     * Sets a parameter previously given to the {@link QueryBuilder} to new values.
     */
    public Query<T> setParameters(Property<?> property, String key, String value) {
        checkOpen();
        nativeSetParameters(handle, property.getEntityId(), property.getId(), null, key, value);
        return this;
    }

    /**
     * Changes the parameters of the query condition with the matching {@code alias} to the new values.
     *
     * @param alias as defined using {@link PropertyQueryCondition#alias(String)}.
     * @param key The first value to use for the query condition.
     * @param value The second value to use for the query condition.
     */
    public Query<T> setParameters(String alias, String key, String value) {
        checkOpen();
        nativeSetParameters(handle, 0, 0, alias, key, value);
        return this;
    }

    /**
     * Sets a parameter previously given to the {@link QueryBuilder} to new values.
     */
    public Query<T> setParameter(Property<?> property, byte[] value) {
        checkOpen();
        nativeSetParameter(handle, property.getEntityId(), property.getId(), null, value);
        return this;
    }

    /**
     * Changes the parameter of the query condition with the matching {@code alias} to a new {@code value}.
     *
     * @param alias as defined using {@link PropertyQueryCondition#alias(String)}.
     * @param value The new value to use for the query condition.
     */
    public Query<T> setParameter(String alias, byte[] value) {
        checkOpen();
        nativeSetParameter(handle, 0, 0, alias, value);
        return this;
    }

    /**
     * For vector search: sets the {@link VectorSearchLevel search level} of the nearest neighbor condition
     * ({@link Property#nearestNeighbors(float[], int)}) of the given vector property, the preferred way to trade search
     * speed for quality (recall). Without it, the search uses the level of the index ({@link HnswIndex#searchLevel()}).
     * <p>
     * The level replaces the oversample and rescore parameters set before; it keeps the limits for searches with other
     * conditions ({@link #setParameterBruteForceMaxCandidates(Property, int)} and
     * {@link #setParameterFilteredSearchMaxNodes(Property, int)}).
     * <p>
     * The other {@code setParameter} methods for vector search set single search parameters instead. They are for
     * power users who tune the search for their data (e.g. by measuring recall and speed on a representative set of
     * queries); the search levels suit most uses. They change one value each, e.g. to adjust a level; a search starts
     * with the values of the search level of the index. A search explores {@code maxResultCount * oversample factor *
     * quantization oversample} candidates (rounded, at least maxResultCount); without quantization, the quantization
     * oversample does not apply.
     *
     * @param property The vector property (a float vector property with an {@link HnswIndex}) of a nearest neighbor
     * condition of this query, like {@code Example_.embedding}.
     * @param level The search level; not {@link VectorSearchLevel#DEFAULT}.
     * @throws IllegalArgumentException if this query has no nearest neighbor condition for the property.
     */
    public Query<T> setParameterSearchLevel(Property<?> property, VectorSearchLevel level) {
        checkOpen();
        nativeSetParameterVectorSearchLevel(handle, property.getEntityId(), property.getId(), null,
                searchLevelNumber(level));
        return this;
    }

    /**
     * Like {@link #setParameterSearchLevel(Property, VectorSearchLevel)}, but for the nearest neighbor condition with
     * the matching {@code alias}.
     *
     * @param alias as defined using {@link NearestNeighborQueryCondition#alias(String)}.
     */
    public Query<T> setParameterSearchLevel(String alias, VectorSearchLevel level) {
        checkOpen();
        nativeSetParameterVectorSearchLevel(handle, 0, 0, alias, searchLevelNumber(level));
        return this;
    }

    /**
     * For vector search: sets how many candidates the nearest neighbor search of the given vector property explores
     * per result for a higher recall, independent of quantization (at least 1). For power users; prefer
     * {@link #setParameterSearchLevel(Property, VectorSearchLevel)}.
     *
     * @param property The vector property (a float vector property with an {@link HnswIndex}) of a nearest neighbor
     * condition of this query, like {@code Example_.embedding}.
     * @throws IllegalArgumentException if this query has no nearest neighbor condition for the property.
     */
    public Query<T> setParameterOversampleFactor(Property<?> property, float factor) {
        checkOpen();
        nativeSetParameterVectorOversampleFactor(handle, property.getEntityId(), property.getId(), null, factor);
        return this;
    }

    /**
     * Like {@link #setParameterOversampleFactor(Property, float)}, but for the nearest neighbor condition with the
     * matching {@code alias}.
     *
     * @param alias as defined using {@link NearestNeighborQueryCondition#alias(String)}.
     */
    public Query<T> setParameterOversampleFactor(String alias, float factor) {
        checkOpen();
        nativeSetParameterVectorOversampleFactor(handle, 0, 0, alias, factor);
        return this;
    }

    /**
     * For vector search with quantization: sets the oversample factor of the nearest neighbor search of the given
     * vector property that corrects for the quantization error only (0 or at least 1); 0 uses the one of the index
     * ({@link HnswIndex#quantizationOversample()}). For power users; prefer
     * {@link #setParameterSearchLevel(Property, VectorSearchLevel)}.
     *
     * @param property The vector property (a float vector property with an {@link HnswIndex}) of a nearest neighbor
     * condition of this query, like {@code Example_.embedding}.
     * @throws IllegalArgumentException if this query has no nearest neighbor condition for the property.
     */
    public Query<T> setParameterQuantizationOversample(Property<?> property, float factor) {
        checkOpen();
        nativeSetParameterVectorQuantizationOversample(handle, property.getEntityId(), property.getId(), null, factor);
        return this;
    }

    /**
     * Like {@link #setParameterQuantizationOversample(Property, float)}, but for the nearest neighbor condition with
     * the matching {@code alias}.
     *
     * @param alias as defined using {@link NearestNeighborQueryCondition#alias(String)}.
     */
    public Query<T> setParameterQuantizationOversample(String alias, float factor) {
        checkOpen();
        nativeSetParameterVectorQuantizationOversample(handle, 0, 0, alias, factor);
        return this;
    }

    /**
     * For vector search with quantization: sets whether the nearest neighbor search of the given vector property
     * rescores the candidates with the full-precision vectors read from the objects (exact result distances and higher
     * recall levels), or returns the nearest by their quantized distance estimates (faster; the result distances are
     * estimates). For power users; prefer {@link #setParameterSearchLevel(Property, VectorSearchLevel)}.
     * <p>
     * A non-zero rescore factor ({@link #setParameterRescoreFactor(Property, float)}) also turns rescoring on; the last
     * call wins, e.g. this turns rescoring off after a factor was set.
     *
     * @param property The vector property (a float vector property with an {@link HnswIndex}) of a nearest neighbor
     * condition of this query, like {@code Example_.embedding}.
     * @throws IllegalArgumentException if this query has no nearest neighbor condition for the property.
     */
    public Query<T> setParameterRescore(Property<?> property, boolean rescore) {
        checkOpen();
        nativeSetParameterVectorRescore(handle, property.getEntityId(), property.getId(), null, rescore);
        return this;
    }

    /**
     * Like {@link #setParameterRescore(Property, boolean)}, but for the nearest neighbor condition with the matching
     * {@code alias}.
     *
     * @param alias as defined using {@link NearestNeighborQueryCondition#alias(String)}.
     */
    public Query<T> setParameterRescore(String alias, boolean rescore) {
        checkOpen();
        nativeSetParameterVectorRescore(handle, 0, 0, alias, rescore);
        return this;
    }

    /**
     * For vector search with quantization: sets how many candidates the nearest neighbor search of the given vector
     * property rescores: this factor times maxResultCount candidates with the nearest estimates (0 or at least 1;
     * capped at the explored candidates); 0 uses the one of the index ({@link HnswIndex#quantizationRescoreFactor()}).
     * Like {@link NearestNeighborQueryCondition#rescoreFactor(float)}, a non-zero factor also turns rescoring on
     * ({@link #setParameterRescore(Property, boolean)}), e.g. for the search levels that do not rescore otherwise
     * (1-3); 0 keeps the rescore setting. For power users; prefer
     * {@link #setParameterSearchLevel(Property, VectorSearchLevel)}.
     *
     * @param property The vector property (a float vector property with an {@link HnswIndex}) of a nearest neighbor
     * condition of this query, like {@code Example_.embedding}.
     * @throws IllegalArgumentException if this query has no nearest neighbor condition for the property.
     */
    public Query<T> setParameterRescoreFactor(Property<?> property, float factor) {
        checkOpen();
        nativeSetParameterVectorRescoreFactor(handle, property.getEntityId(), property.getId(), null, factor);
        return this;
    }

    /**
     * Like {@link #setParameterRescoreFactor(Property, float)}, but for the nearest neighbor condition with the
     * matching {@code alias}.
     *
     * @param alias as defined using {@link NearestNeighborQueryCondition#alias(String)}.
     */
    public Query<T> setParameterRescoreFactor(String alias, float factor) {
        checkOpen();
        nativeSetParameterVectorRescoreFactor(handle, 0, 0, alias, factor);
        return this;
    }

    /**
     * For vector search with other conditions: sets up to how many candidates of their lookups (indexes and relations)
     * the nearest neighbor search of the given vector property finds the nearest neighbors by brute force (exact
     * without quantization) instead of by a graph search with a filter; overrides the value of the index
     * ({@link HnswIndex#bruteForceMaxCandidates()}), 0 uses it.
     * <p>
     * Higher values let more queries use brute force, but its costs grow with the candidates, while the graph search
     * gets cheaper the more objects match (e.g. on 290k objects, the graph search was faster from about 15k
     * candidates, or from about 5k if the other conditions need to be checked on the objects).
     *
     * @param property The vector property (a float vector property with an {@link HnswIndex}) of a nearest neighbor
     * condition of this query, like {@code Example_.embedding}.
     * @throws IllegalArgumentException if this query has no nearest neighbor condition for the property.
     */
    public Query<T> setParameterBruteForceMaxCandidates(Property<?> property, int maxCandidates) {
        checkOpen();
        nativeSetParameterVectorBruteForceMaxCandidates(handle, property.getEntityId(), property.getId(), null,
                maxCandidates);
        return this;
    }

    /**
     * Like {@link #setParameterBruteForceMaxCandidates(Property, int)}, but for the nearest neighbor condition with the
     * matching {@code alias}.
     *
     * @param alias as defined using {@link NearestNeighborQueryCondition#alias(String)}.
     */
    public Query<T> setParameterBruteForceMaxCandidates(String alias, int maxCandidates) {
        checkOpen();
        nativeSetParameterVectorBruteForceMaxCandidates(handle, 0, 0, alias, maxCandidates);
        return this;
    }

    /**
     * For vector search with other conditions: sets how many nodes the nearest neighbor search of the given vector
     * property explores at most in a graph search with a filter (for the matching objects); overrides the value of the
     * index ({@link HnswIndex#filteredSearchMaxNodes()}), 0 uses it.
     * <p>
     * Higher values increase the recall if few objects match, at the cost of speed; at the limit, fewer than
     * maxResultCount objects may be found.
     *
     * @param property The vector property (a float vector property with an {@link HnswIndex}) of a nearest neighbor
     * condition of this query, like {@code Example_.embedding}.
     * @throws IllegalArgumentException if this query has no nearest neighbor condition for the property.
     */
    public Query<T> setParameterFilteredSearchMaxNodes(Property<?> property, int maxNodes) {
        checkOpen();
        nativeSetParameterVectorFilteredSearchMaxNodes(handle, property.getEntityId(), property.getId(), null,
                maxNodes);
        return this;
    }

    /**
     * Like {@link #setParameterFilteredSearchMaxNodes(Property, int)}, but for the nearest neighbor condition with the
     * matching {@code alias}.
     *
     * @param alias as defined using {@link NearestNeighborQueryCondition#alias(String)}.
     */
    public Query<T> setParameterFilteredSearchMaxNodes(String alias, int maxNodes) {
        checkOpen();
        nativeSetParameterVectorFilteredSearchMaxNodes(handle, 0, 0, alias, maxNodes);
        return this;
    }

    /**
     * Returns the level number (1-10) of a search level for the natives.
     *
     * @throws IllegalArgumentException if the level is null or {@link VectorSearchLevel#DEFAULT}.
     */
    static int searchLevelNumber(@Nullable VectorSearchLevel level) {
        if (level == null || level == VectorSearchLevel.DEFAULT) {
            throw new IllegalArgumentException("A query requires a search level from L1 to L10, but got " + level);
        }
        return level.ordinal(); // The ordinal is the level
    }

    /**
     * Removes (deletes) all Objects matching the query
     *
     * @return count of removed Objects
     */
    public long remove() {
        checkOpen();
        ensureNoFilter();
        return box.internalCallWithWriterHandle(cursorHandle -> nativeRemove(handle, cursorHandle));
    }

    /**
     * Returns a {@link SubscriptionBuilder} to build a subscription to observe changes to the results of this query.
     * <p>
     * Typical usage:
     * <pre>
     * DataSubscription subscription = query.subscribe()
     *         .observer((List&lt;T&gt; data) -> {
     *               // Do something with the returned results
     *         });
     * // Once the observer should no longer be notified
     * subscription.cancel();
     * </pre>
     * Note that the observer will receive new results on any changes to the {@link Box} of the {@link Entity @Entity}
     * this queries, regardless of the conditions of this query. This is because the {@link QueryPublisher} used for the
     * subscription observes changes by using {@link BoxStore#subscribe(Class)} on the Box this queries.
     * <p>
     * To customize this or for advanced use cases, consider using {@link BoxStore#subscribe(Class)} directly.
     * <p>
     * See {@link SubscriptionBuilder#observer(DataObserver)} for additional details.
     *
     * @return A {@link SubscriptionBuilder} to build a subscription.
     * @see #publish()
     */
    public SubscriptionBuilder<List<T>> subscribe() {
        checkOpen();
        return new SubscriptionBuilder<>(publisher, null);
    }

    /**
     * Convenience for {@link #subscribe()} with a subsequent call to
     * {@link SubscriptionBuilder#dataSubscriptionList(DataSubscriptionList)}.
     *
     * @param dataSubscriptionList the resulting {@link io.objectbox.reactive.DataSubscription} will be added to it
     */
    public SubscriptionBuilder<List<T>> subscribe(DataSubscriptionList dataSubscriptionList) {
        SubscriptionBuilder<List<T>> subscriptionBuilder = subscribe();
        subscriptionBuilder.dataSubscriptionList(dataSubscriptionList);
        return subscriptionBuilder;
    }

    /**
     * Manually schedules publishing the current results of this query to all {@link #subscribe() subscribed}
     * {@link DataObserver observers}, even if the underlying Boxes have not changed.
     * <p>
     * This is useful to publish new results after changing parameters of this query which would otherwise not trigger
     * publishing of new results.
     */
    public void publish() {
        // Do open check to not silently fail (publisher runnable would just not get scheduled if query is closed)
        checkOpen();
        publisher.publish();
    }

    /**
     * For logging and testing, returns a string describing this query
     * like "Query for entity Example with 4 conditions with properties prop1, prop2".
     * <p>
     * Note: the format of the returned string may change without notice.
     */
    public String describe() {
        checkOpen();
        return nativeToString(handle);
    }

    /**
     * For logging and testing, returns a string describing the conditions of this query
     * like "(prop1 == A AND prop2 is null)".
     * <p>
     * Note: the format of the returned string may change without notice.
     */
    public String describeParameters() {
        checkOpen();
        return nativeDescribeParameters(handle);
    }

    /**
     * Throws if {@link #close()} has been called for this.
     */
    private void checkOpen() {
        if (handle == 0) {
            throw new IllegalStateException("This query is closed. Build and use a new one.");
        }
    }

}
