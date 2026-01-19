/*
 * Copyright 2017-2025 ObjectBox Ltd.
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

package io.objectbox;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import io.objectbox.annotation.Backlink;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.apihint.Experimental;
import io.objectbox.annotation.apihint.Internal;
import io.objectbox.exception.DbException;
import io.objectbox.internal.CallWithHandle;
import io.objectbox.internal.IdGetter;
import io.objectbox.internal.ReflectionCache;
import io.objectbox.query.QueryBuilder;
import io.objectbox.query.QueryCondition;
import io.objectbox.relation.RelationInfo;
import io.objectbox.relation.ToMany;
import io.objectbox.relation.ToOne;

/**
 * A Box to put and get Objects of a specific Entity class.
 * <p>
 * Thread-safe.
 */
@ThreadSafe
@SuppressWarnings("WeakerAccess,UnusedReturnValue,unused")
public class Box<T> {
    private final BoxStore store;
    private final Class<T> entityClass;

    /** Set when running inside TX */
    final ThreadLocal<Cursor<T>> activeTxCursor = new ThreadLocal<>();
    private final ThreadLocal<Cursor<T>> threadLocalReader = new ThreadLocal<>();

    private final IdGetter<T> idGetter;

    private EntityInfo<T> entityInfo;
    private volatile Field boxStoreField;

    Box(BoxStore store, Class<T> entityClass) {
        this.store = store;
        this.entityClass = entityClass;
        idGetter = store.getEntityInfo(entityClass).getIdGetter();
    }

    Cursor<T> getReader() {
        Cursor<T> cursor = getActiveTxCursor();
        if (cursor != null) {
            return cursor;
        } else {
            cursor = threadLocalReader.get();
            if (cursor != null) {
                Transaction tx = cursor.tx;
                if (tx.isClosed() || !tx.isRecycled()) {
                    throw new IllegalStateException("Illegal reader TX state");
                }
                tx.renew();
                cursor.renew();
            } else {
                cursor = store.beginReadTx().createCursor(entityClass);
                threadLocalReader.set(cursor);
            }
        }
        return cursor;
    }

    Cursor<T> getActiveTxCursor() {
        Transaction activeTx = store.activeTx.get();
        if (activeTx != null) {
            if (activeTx.isClosed()) {
                throw new IllegalStateException("Active TX is closed");
            }
            Cursor<T> cursor = activeTxCursor.get();
            if (cursor == null || cursor.getTx().isClosed()) {
                cursor = activeTx.createCursor(entityClass);
                activeTxCursor.set(cursor);
            }
            return cursor;
        }
        return null;
    }

    Cursor<T> getWriter() {
        Cursor<T> cursor = getActiveTxCursor();
        if (cursor != null) {
            return cursor;
        } else {
            Transaction tx = store.beginTx();
            try {
                return tx.createCursor(entityClass);
            } catch (RuntimeException e) {
                tx.close();
                throw e;
            }
        }
    }

    void commitWriter(Cursor<T> cursor) {
        // NOP if TX is ongoing
        if (activeTxCursor.get() == null) {
            cursor.close();
            cursor.getTx().commitAndClose();
        }
    }

    void releaseWriter(Cursor<T> cursor) {
        // NOP if TX is ongoing
        if (activeTxCursor.get() == null) {
            Transaction tx = cursor.getTx();
            if (!tx.isClosed()) {
                cursor.close();
                tx.abort();
                tx.close();
            }
        }
    }

    void releaseReader(Cursor<T> cursor) {
        // NOP if TX is ongoing
        if (activeTxCursor.get() == null) {
            Transaction tx = cursor.getTx();
            if (tx.isClosed() || tx.isRecycled() || !tx.isReadOnly()) {
                throw new IllegalStateException("Illegal reader TX state");
            }
            tx.recycle();
        }
    }

    /**
     * Like {@link BoxStore#closeThreadResources()}, but limited to only this Box.
     * <p>
     * Rule of thumb: prefer {@link BoxStore#closeThreadResources()} unless you know that your thread only interacted
     * with this Box.
     */
    public void closeThreadResources() {
        Cursor<T> cursor = threadLocalReader.get();
        if (cursor != null) {
            cursor.close();
            cursor.getTx().close(); // a read TX is always started when the threadLocalReader is set
            threadLocalReader.remove();
        }
    }

    /**
     * Returns if for the calling thread this has a reader Cursor.
     */
    boolean hasReaderCursorForCurrentThread() {
        return threadLocalReader.get() != null;
    }

    /**
     * If there is one, and it was created using the given {@code tx}, removes and closes the {@link #activeTxCursor}
     * for the current thread.
     * <p>
     * This should be called before the active transaction is closed to clean up native resources.
     * <p>
     * Note: {@link #threadLocalReader} is either renewed by the next call to {@link #getReader()} or cleaned up by
     * {@link #closeThreadResources()}.
     */
    void closeActiveTxCursorForCurrentThread(Transaction tx) {
        Cursor<T> cursor = activeTxCursor.get();
        if (cursor != null && cursor.getTx() == tx) {
            activeTxCursor.remove();
            cursor.close();
        }
    }

    /**
     * Returns if for the calling thread this has a Cursor, if any, for the currently active transaction.
     */
    boolean hasActiveTxCursorForCurrentThread() {
        return activeTxCursor.get() != null;
    }

    /** Used by tests */
    int getPropertyId(String propertyName) {
        Cursor<T> reader = getReader();
        try {
            return reader.getPropertyId(propertyName);
        } finally {
            releaseReader(reader);
        }
    }

    @Internal
    public long getId(T entity) {
        return idGetter.getId(entity);
    }

    /**
     * Get the stored object for the given ID.
     *
     * @return null if not found
     */
    public T get(long id) {
        Cursor<T> reader = getReader();
        try {
            return reader.get(id);
        } finally {
            releaseReader(reader);
        }
    }

    /**
     * Get the stored objects for the given IDs.
     *
     * @return null if not found
     */
    public List<T> get(Iterable<Long> ids) {
        ArrayList<T> list = new ArrayList<>();
        Cursor<T> reader = getReader();
        try {
            for (Long id : ids) {
                T entity = reader.get(id);
                if (entity != null) {
                    list.add(entity);
                }
            }
        } finally {
            releaseReader(reader);
        }
        return list;
    }

    /**
     * Get the stored objects for the given IDs.
     *
     * @return null if not found
     */
    public List<T> get(long[] ids) {
        ArrayList<T> list = new ArrayList<>(ids.length);
        Cursor<T> reader = getReader();
        try {
            for (Long id : ids) {
                T entity = reader.get(id);
                if (entity != null) {
                    list.add(entity);
                }
            }
        } finally {
            releaseReader(reader);
        }
        return list;
    }

    /**
     * Get the stored objects for the given IDs as a Map with IDs as keys, and entities as values.
     * IDs for which no entity is found will be put in the map with null values.
     *
     * @return null if not found
     */
    public Map<Long, T> getMap(Iterable<Long> ids) {
        HashMap<Long, T> map = new HashMap<>();
        Cursor<T> reader = getReader();
        try {
            for (Long id : ids) {
                map.put(id, reader.get(id));
            }
        } finally {
            releaseReader(reader);
        }
        return map;
    }

    /**
     * Returns the count of all stored objects in this box.
     */
    public long count() {
        return count(0);
    }

    /**
     * Returns the count of all stored objects in this box or the given maxCount, whichever is lower.
     *
     * @param maxCount maximum value to count or 0 (zero) to have no maximum limit
     */
    public long count(long maxCount) {
        Cursor<T> reader = getReader();
        try {
            return reader.count(maxCount);
        } finally {
            releaseReader(reader);
        }
    }

    /** Returns true if no objects are in this box. */
    public boolean isEmpty() {
        return count(1) == 0;
    }

    /**
     * Returns all stored Objects in this Box.
     *
     * @return since 2.4 the returned list is always mutable (before an empty result list was immutable)
     */
    public List<T> getAll() {
        ArrayList<T> list = new ArrayList<>();
        Cursor<T> cursor = getReader();
        try {
            for (T object = cursor.first(); object != null; object = cursor.next()) {
                list.add(object);
            }
            return list;
        } finally {
            releaseReader(cursor);
        }
    }

    /**
     * Check if an object with the given ID exists in the database.
     * This is more efficient than a {@link #get(long)} and comparing against null.
     *
     * @return true if an object with the given ID was found, false otherwise.
     * @since 2.7
     */
    public boolean contains(long id) {
        Cursor<T> reader = getReader();
        try {
            return reader.seek(id);
        } finally {
            releaseReader(reader);
        }
    }

    /**
     * Puts the given object and returns its (new) ID.
     * <p>
     * This means that if its {@link Id @Id} property is 0 or null, it is inserted as a new object and assigned the next
     * available ID. For example, if there is an object with ID 1 and another with ID 100, it will be assigned ID 101.
     * The new ID is also set on the given object before this returns.
     * <p>
     * If instead the object has an assigned ID set, if an object with the same ID exists it is updated. Otherwise, it
     * is inserted with that ID.
     * <p>
     * If the ID was not assigned before an {@link IllegalArgumentException} is thrown.
     * <p>
     * When the object contains {@link ToOne} or {@link ToMany} relations, they are created (or updated) to point to the
     * (new) target objects. The target objects themselves are typically not updated or removed. To do so, put or remove
     * them using their {@link Box}. However, for convenience, if a target object is new, it will be inserted and
     * assigned an ID in its Box before creating or updating the relation. Also, for ToMany relations based on a
     * {@link Backlink} the target objects are updated (to store changes in the linked ToOne or ToMany relation).
     * <p>
     * Performance note: if you want to put several objects, consider {@link #put(Collection)}, {@link #put(Object[])},
     * {@link BoxStore#runInTx(Runnable)}, etc. instead.
     */
    public long put(T entity) {
        Cursor<T> cursor = getWriter();
        try {
            long key = cursor.put(entity);
            commitWriter(cursor);
            return key;
        } finally {
            releaseWriter(cursor);
        }
    }

    /**
     * Puts the given entities in a box using a single transaction.
     * <p>
     * See {@link #put(Object)} for more details.
     */
    @SafeVarargs // Not using T... as Object[], no ClassCastException expected.
    public final void put(@Nullable T... entities) {
        if (entities == null || entities.length == 0) {
            return;
        }

        Cursor<T> cursor = getWriter();
        try {
            for (T entity : entities) {
                cursor.put(entity);
            }
            commitWriter(cursor);
        } finally {
            releaseWriter(cursor);
        }
    }

    /**
     * Puts the given entities in a box using a single transaction.
     * <p>
     * See {@link #put(Object)} for more details.
     *
     * @param entities It is fine to pass null or an empty collection:
     *                 this case is handled efficiently without overhead.
     */
    public void put(@Nullable Collection<T> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }

        Cursor<T> cursor = getWriter();
        try {
            for (T entity : entities) {
                cursor.put(entity);
            }
            commitWriter(cursor);
        } finally {
            releaseWriter(cursor);
        }
    }

    /**
     * Puts the given entities in a box in batches using a separate transaction for each batch.
     * <p>
     * See {@link #put(Object)} for more details.
     *
     * @param entities  It is fine to pass null or an empty collection:
     *                  this case is handled efficiently without overhead.
     * @param batchSize Number of entities that will be put in one transaction. Must be 1 or greater.
     */
    public void putBatched(@Nullable Collection<T> entities, int batchSize) {
        if (batchSize < 1) {
            throw new IllegalArgumentException("Batch size must be 1 or greater but was " + batchSize);
        }
        if (entities == null) {
            return;
        }

        Iterator<T> iterator = entities.iterator();
        while (iterator.hasNext()) {
            Cursor<T> cursor = getWriter();
            try {
                int number = 0;
                while (number++ < batchSize && iterator.hasNext()) {
                    cursor.put(iterator.next());
                }
                commitWriter(cursor);
            } finally {
                releaseWriter(cursor);
            }
        }
    }

    /**
     * Removes (deletes) the object with the given ID.
     * <p>
     * If the object is part of a relation, it will be removed from that relation as well.
     *
     * @return true if the object did exist and was removed, otherwise false.
     */
    public boolean remove(long id) {
        Cursor<T> cursor = getWriter();
        boolean removed;
        try {
            removed = cursor.deleteEntity(id);
            commitWriter(cursor);
        } finally {
            releaseWriter(cursor);
        }
        return removed;
    }

    /**
     * Like {@link #remove(long)}, but removes multiple objects in a single transaction.
     */
    public void remove(@Nullable long... ids) {
        if (ids == null || ids.length == 0) {
            return;
        }

        Cursor<T> cursor = getWriter();
        try {
            for (long key : ids) {
                cursor.deleteEntity(key);
            }
            commitWriter(cursor);
        } finally {
            releaseWriter(cursor);
        }
    }

    /**
     * Like {@link #remove(long)}, but removes multiple objects in a single transaction.
     */
    public void removeByIds(@Nullable Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        Cursor<T> cursor = getWriter();
        try {
            for (long key : ids) {
                cursor.deleteEntity(key);
            }
            commitWriter(cursor);
        } finally {
            releaseWriter(cursor);
        }
    }

    /**
     * Like {@link #remove(long)}, but obtains the ID from the {@link Id @Id} property of the given object instead.
     */
    public boolean remove(T object) {
        Cursor<T> cursor = getWriter();
        boolean removed;
        try {
            long id = cursor.getId(object);
            removed = cursor.deleteEntity(id);
            commitWriter(cursor);
        } finally {
            releaseWriter(cursor);
        }
        return removed;
    }

    /**
     * Like {@link #remove(Object)}, but removes multiple objects in a single transaction.
     */
    @SafeVarargs // Not using T... as Object[], no ClassCastException expected.
    @SuppressWarnings("Duplicates") // Detected duplicate has different type
    public final void remove(@Nullable T... objects) {
        if (objects == null || objects.length == 0) {
            return;
        }
        Cursor<T> cursor = getWriter();
        try {
            for (T entity : objects) {
                long key = cursor.getId(entity);
                cursor.deleteEntity(key);
            }
            commitWriter(cursor);
        } finally {
            releaseWriter(cursor);
        }
    }

    /**
     * Like {@link #remove(Object)}, but removes multiple objects in a single transaction.
     */
    @SuppressWarnings("Duplicates") // Detected duplicate has different type
    public void remove(@Nullable Collection<T> objects) {
        if (objects == null || objects.isEmpty()) {
            return;
        }
        Cursor<T> cursor = getWriter();
        try {
            for (T entity : objects) {
                long key = cursor.getId(entity);
                cursor.deleteEntity(key);
            }
            commitWriter(cursor);
        } finally {
            releaseWriter(cursor);
        }
    }

    /**
     * Like {@link #remove(long)}, but removes <b>all</b> objects in a single transaction.
     */
    public void removeAll() {
        Cursor<T> cursor = getWriter();
        try {
            cursor.deleteAll();
            commitWriter(cursor);
        } finally {
            releaseWriter(cursor);
        }
    }

    /**
     * WARNING: this method should generally be avoided as it is not transactional and thus may leave the DB in an
     * inconsistent state. It may be the a last resort option to recover from a full DB.
     * Like removeAll(), it removes all objects, returns the count of objects removed.
     * Logs progress using warning log level.
     */
    @Experimental
    public long panicModeRemoveAll() {
        return store.panicModeRemoveAllObjects(getEntityInfo().getEntityId());
    }

    /**
     * Create a query with no conditions.
     *
     * @see #query(QueryCondition)
     */
    public QueryBuilder<T> query() {
        return new QueryBuilder<>(this, store.getNativeStore(), store.getDbName(entityClass));
    }

    /**
     * Applies the given query conditions and returns the builder for further customization, such as result order.
     * Build the condition using the properties from your entity underscore classes.
     * <p>
     * An example with a nested OR condition:
     * <pre>
     * # Java
     * box.query(User_.name.equal("Jane")
     *         .and(User_.age.less(12)
     *                 .or(User_.status.equal("child"))));
     *
     * # Kotlin
     * box.query(User_.name.equal("Jane")
     *         and (User_.age.less(12)
     *         or User_.status.equal("child")))
     * </pre>
     * This method is a shortcut for {@code query().apply(condition)}.
     *
     * @see QueryBuilder#apply(QueryCondition)
     */
    public QueryBuilder<T> query(QueryCondition<T> queryCondition) {
        return query().apply(queryCondition);
    }

    public BoxStore getStore() {
        return store;
    }

    public synchronized EntityInfo<T> getEntityInfo() {
        if (entityInfo == null) {
            Cursor<T> reader = getReader();
            try {
                entityInfo = reader.getEntityInfo();
            } finally {
                releaseReader(reader);
            }
        }
        return entityInfo;
    }

    /**
     * Attaches the given object to this.
     * <p>
     * This typically should only be used when <a
     * href="https://docs.objectbox.io/advanced/object-ids#self-assigned-object-ids">manually assigning IDs</a>.
     *
     * @param entity The object to attach this to.
     */
    public void attach(T entity) {
        if (boxStoreField == null) {
            try {
                boxStoreField = ReflectionCache.getInstance().getField(entityClass, "__boxStore");
            } catch (Exception e) {
                throw new DbException("Entity cannot be attached - only active entities with relationships support " +
                        "attaching (class has no __boxStore field(?)) : " + entityClass, e);
            }
        }
        try {
            boxStoreField.set(entity, store);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    // Sketching future API extension
    private boolean isChanged(T entity) {
        return false;
    }

    // Sketching future API extension
    private boolean putIfChanged(T entity) {
        return false;
    }

    public Class<T> getEntityClass() {
        return entityClass;
    }

    @Internal
    public List<T> internalGetBacklinkEntities(int entityId, Property<?> relationIdProperty, long key) {
        Cursor<T> reader = getReader();
        try {
            return reader.getBacklinkEntities(entityId, relationIdProperty, key);
        } finally {
            releaseReader(reader);
        }
    }

    @Internal
    public List<T> internalGetRelationEntities(int sourceEntityId, int relationId, long key, boolean backlink) {
        Cursor<T> reader = getReader();
        try {
            return reader.getRelationEntities(sourceEntityId, relationId, key, backlink);
        } finally {
            releaseReader(reader);
        }
    }

    @Internal
    public long[] internalGetRelationIds(int sourceEntityId, int relationId, long key, boolean backlink) {
        Cursor<T> reader = getReader();
        try {
            return reader.getRelationIds(sourceEntityId, relationId, key, backlink);
        } finally {
            releaseReader(reader);
        }
    }

    /**
     * Given a ToMany relation and the ID of a source entity gets the target entities of the relation from their box,
     * for example {@code orderBox.getRelationEntities(Customer_.orders, customer.getId())}.
     */
    public List<T> getRelationEntities(RelationInfo<?, T> relationInfo, long id) {
        return internalGetRelationEntities(relationInfo.sourceInfo.getEntityId(), relationInfo.relationId, id, false);
    }

    /**
     * Given a ToMany relation and the ID of a target entity gets all source entities pointing to this target entity,
     * for example {@code customerBox.getRelationEntities(Customer_.orders, order.getId())}.
     */
    public List<T> getRelationBacklinkEntities(RelationInfo<T, ?> relationInfo, long id) {
        return internalGetRelationEntities(relationInfo.sourceInfo.getEntityId(), relationInfo.relationId, id, true);
    }

    /**
     * Like {@link #getRelationEntities(RelationInfo, long)}, but only returns the IDs of the target entities.
     */
    public long[] getRelationIds(RelationInfo<?, T> relationInfo, long id) {
        return internalGetRelationIds(relationInfo.sourceInfo.getEntityId(), relationInfo.relationId, id, false);
    }

    /**
     * Like {@link #getRelationBacklinkEntities(RelationInfo, long)}, but only returns the IDs of the source entities.
     */
    public long[] getRelationBacklinkIds(RelationInfo<T, ?> relationInfo, long id) {
        return internalGetRelationIds(relationInfo.sourceInfo.getEntityId(), relationInfo.relationId, id, true);
    }

    @Internal
    public <RESULT> RESULT internalCallWithReaderHandle(CallWithHandle<RESULT> task) {
        Cursor<T> reader = getReader();
        try {
            return task.call(reader.internalHandle());
        } finally {
            releaseReader(reader);
        }
    }

    @Internal
    public <RESULT> RESULT internalCallWithWriterHandle(CallWithHandle<RESULT> task) {
        Cursor<T> writer = getWriter();
        RESULT result;
        try {
            result = task.call(writer.internalHandle());
            commitWriter(writer);
        } finally {
            releaseWriter(writer);
        }
        return result;
    }

    public String getReaderDebugInfo() {
        Cursor<T> reader = getReader();
        try {
            return reader + " with " + reader.getTx() + "; store's commit count: " + getStore().commitCount;
        } finally {
            releaseReader(reader);
        }
    }

}