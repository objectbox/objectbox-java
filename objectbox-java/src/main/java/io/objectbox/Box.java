/*
 * Copyright 2017-2019 ObjectBox Ltd. All rights reserved.
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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import io.objectbox.annotation.apihint.Beta;
import io.objectbox.annotation.apihint.Experimental;
import io.objectbox.annotation.apihint.Internal;
import io.objectbox.exception.DbException;
import io.objectbox.internal.CallWithHandle;
import io.objectbox.internal.IdGetter;
import io.objectbox.internal.ReflectionCache;
import io.objectbox.query.QueryBuilder;
import io.objectbox.relation.RelationInfo;

/**
 * A box to store objects of a particular class.
 * <p>
 * Thread-safe.
 */
@Beta
@ThreadSafe
@SuppressWarnings("WeakerAccess,UnusedReturnValue,unused")
public class Box<T> {
    private final BoxStore store;
    private final Class<T> entityClass;

    /** Set when running inside TX */
    final ThreadLocal<Cursor<T>> activeTxCursor = new ThreadLocal<>();
    private final ThreadLocal<Cursor<T>> threadLocalReader = new ThreadLocal<>();

    private final IdGetter<T> idGetter;

    private EntityInfo entityInfo;
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

    void txCommitted(Transaction tx) {
        // Thread local readers will be renewed on next get, so we do not need clean them up

        Cursor<T> cursor = activeTxCursor.get();
        if (cursor != null) {
            activeTxCursor.remove();
            cursor.close();
        }
    }

    /**
     * Called by {@link BoxStore#callInReadTx(Callable)} - does not throw so caller does not need try/finally.
     */
    void readTxFinished(Transaction tx) {
        Cursor<T> cursor = activeTxCursor.get();
        if (cursor != null && cursor.getTx() == tx) {
            activeTxCursor.remove();
            cursor.close();
        }
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
     * Puts the given object in the box (aka persisting it). If this is a new entity (its ID property is 0), a new ID
     * will be assigned to the entity (and returned). If the entity was already put in the box before, it will be
     * overwritten.
     * <p>
     * Performance note: if you want to put several entities, consider {@link #put(Collection)},
     * {@link #put(Object[])}, {@link BoxStore#runInTx(Runnable)}, etc. instead.
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
     */
    public void put(@Nullable T... entities) {
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
     * Removes (deletes) the Object by its ID.
     * @return true if an entity was actually removed (false if no entity exists with the given ID)
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
     * Removes (deletes) Objects by their ID in a single transaction.
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

    @Deprecated
    /** @deprecated use {@link #removeByIds(Collection)} instead. */
    public void removeByKeys(@Nullable Collection<Long> ids) {
        removeByIds(ids);
    }

    /**
     * Due to type erasure collision, we cannot simply use "remove" as a method name here.
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
     * Removes (deletes) the given Object.
     * @return true if an entity was actually removed (false if no entity exists with the given ID)
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
     * Removes (deletes) the given Objects in a single transaction.
     */
    @SuppressWarnings("Duplicates") // Detected duplicate has different type
    public void remove(@Nullable T... objects) {
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
     * Removes (deletes) the given Objects in a single transaction.
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
     * Removes (deletes) ALL Objects in a single transaction.
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
     * Returns a builder to create queries for Object matching supplied criteria.
     */
    public QueryBuilder<T> query() {
        return new QueryBuilder<>(this, store.internalHandle(), store.getDbName(entityClass));
    }

    public BoxStore getStore() {
        return store;
    }

    public synchronized EntityInfo getEntityInfo() {
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

    @Beta
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
    public List<T> internalGetBacklinkEntities(int entityId, Property relationIdProperty, long key) {
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