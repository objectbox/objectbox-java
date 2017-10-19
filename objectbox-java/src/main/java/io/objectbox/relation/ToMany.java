/*
 * Copyright (C) 2017 Markus Junginger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.objectbox.relation;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import io.objectbox.Box;
import io.objectbox.BoxStore;
import io.objectbox.Cursor;
import io.objectbox.InternalAccess;
import io.objectbox.annotation.apihint.Beta;
import io.objectbox.annotation.apihint.Experimental;
import io.objectbox.annotation.apihint.Internal;
import io.objectbox.exception.DbDetachedException;
import io.objectbox.internal.IdGetter;
import io.objectbox.internal.ReflectionCache;
import io.objectbox.query.QueryFilter;
import io.objectbox.relation.ListFactory.CopyOnWriteArrayListFactory;

/**
 * A List representing a to-many relation.
 * It tracks changes (adds and removes) that can be later applied (persisted) to the database.
 * This happens either on {@link Box#put(Object)} of the source entity of this relation or using
 * {@link #applyChangesToDb()}.
 * <p>
 * If this relation is a backlink from a {@link ToOne} relation, a DB sync will also update ToOne objects
 * (but not vice versa).
 * <p>
 * ToMany is thread-safe by default (only if the default {@link java.util.concurrent.CopyOnWriteArrayList} is used).
 *
 * @param <TARGET> Object type (entity).
 */
public class ToMany<TARGET> implements List<TARGET>, Serializable {
    private static final long serialVersionUID = 2367317778240689006L;

    private final Object entity;
    private final RelationInfo<TARGET> relationInfo;

    private ListFactory listFactory;
    private List<TARGET> entities;

    /** Entities added since last put/sync. Map is used as a set (value is always Boolean.TRUE). */
    private Map<TARGET, Boolean> entitiesAdded;

    /** Entities removed since last put/sync. Map is used as a set (value is always Boolean.TRUE). */
    private Map<TARGET, Boolean> entitiesRemoved;

    List<TARGET> entitiesToPut;
    List<TARGET> entitiesToRemove;

    transient private BoxStore boxStore;
    transient private Box entityBox;
    transient private volatile Box<TARGET> targetBox;
    transient private boolean removeFromTargetBox;
    transient private Comparator<TARGET> comparator;

    public ToMany(Object sourceEntity, RelationInfo<TARGET> relationInfo) {
        if (sourceEntity == null) {
            throw new IllegalArgumentException("No source entity given (null)");
        }
        if (relationInfo == null) {
            throw new IllegalArgumentException("No relation info given (null)");
        }
        this.entity = sourceEntity;
        this.relationInfo = relationInfo;
    }

    /** Currently only used for non-persisted entities (id == 0). */
    @Experimental
    public void setListFactory(ListFactory listFactory) {
        if (listFactory == null) {
            throw new IllegalArgumentException("ListFactory is null");
        }
        this.listFactory = listFactory;
    }

    /** Set an comparator to define the order of entities. */
    @Experimental
    public void setComparator(Comparator<TARGET> comparator) {
        this.comparator = comparator;
    }

    /**
     * On put, this also deletes removed entities from the target Box.
     * Note: removed target entities won't cascade the delete.
     */
    @Experimental
    public synchronized void setRemoveFromTargetBox(boolean removeFromTargetBox) {
        this.removeFromTargetBox = removeFromTargetBox;
    }

    public ListFactory getListFactory() {
        if (listFactory == null) {
            synchronized (this) {
                if (listFactory == null) {
                    listFactory = new CopyOnWriteArrayListFactory();
                }
            }
        }
        return listFactory;
    }

    private void ensureBoxes() {
        if (targetBox == null) {
            Field boxStoreField = ReflectionCache.getInstance().getField(entity.getClass(), "__boxStore");
            try {
                boxStore = (BoxStore) boxStoreField.get(entity);
                if (boxStore == null) {
                    throw new DbDetachedException("Cannot resolve relation for detached entities");
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            entityBox = boxStore.boxFor(relationInfo.sourceInfo.getEntityClass());
            targetBox = boxStore.boxFor(relationInfo.targetInfo.getEntityClass());
        }
    }

    private void ensureEntitiesWithTrackingLists() {
        ensureEntities();
        if (entitiesAdded == null) {
            synchronized (this) {
                if (entitiesAdded == null) {
                    entitiesAdded = new LinkedHashMap<>(); // Keep order of added items
                    entitiesRemoved = new LinkedHashMap<>(); // Keep order of added items
                }
            }
        }
    }

    private void ensureEntities() {
        if (entities == null) {
            long id = relationInfo.sourceInfo.getIdGetter().getId(entity);
            if (id == 0) {
                // Not yet persisted entity
                synchronized (this) {
                    if (entities == null) {
                        entities = getListFactory().createList();
                    }
                }
            } else {
                ensureBoxes();
                List<TARGET> newEntities;
                int relationId = relationInfo.relationId;
                if (relationId != 0) {
                    int sourceEntityId = relationInfo.sourceInfo.getEntityId();
                    newEntities = targetBox.internalGetRelationEntities(sourceEntityId, relationId, id);
                } else {
                    newEntities = targetBox.internalGetBacklinkEntities(relationInfo.targetInfo.getEntityId(),
                            relationInfo.targetIdProperty, id);
                }
                if (comparator != null) {
                    Collections.sort(newEntities, comparator);
                }
                synchronized (this) {
                    if (entities == null) {
                        entities = newEntities;
                    }
                }
            }
        }
    }

    @Override
    /**
     * Adds the given entity to the list and tracks the addition so it can be later applied to the database
     * (e.g. via {@link Box#put(Object)} of the entity owning the ToMany, or via {@link #applyChangesToDb()}).
     * Note that the given entity will remain unchanged at this point (e.g. to-ones are not updated).
     */
    public synchronized boolean add(TARGET object) {
        ensureEntitiesWithTrackingLists();
        entitiesAdded.put(object, Boolean.TRUE);
        entitiesRemoved.remove(object);
        return entities.add(object);
    }

    @Override
    /** See {@link #add(Object)} for general comments. */
    public synchronized void add(int location, TARGET object) {
        ensureEntitiesWithTrackingLists();
        entitiesAdded.put(object, Boolean.TRUE);
        entitiesRemoved.remove(object);
        entities.add(location, object);
    }

    @Override
    /** See {@link #add(Object)} for general comments. */
    public synchronized boolean addAll(Collection<? extends TARGET> objects) {
        putAllToAdded(objects);
        return entities.addAll(objects);
    }

    private synchronized void putAllToAdded(Collection<? extends TARGET> objects) {
        ensureEntitiesWithTrackingLists();
        for (TARGET object : objects) {
            entitiesAdded.put(object, Boolean.TRUE);
            entitiesRemoved.remove(object);
        }
    }

    @Override
    /** See {@link #add(Object)} for general comments. */
    public synchronized boolean addAll(int index, Collection<? extends TARGET> objects) {
        putAllToAdded(objects);
        return entities.addAll(index, objects);
    }

    @Override
    public synchronized void clear() {
        ensureEntitiesWithTrackingLists();
        List<TARGET> entitiesToClear = entities;
        if (entitiesToClear != null) {
            for (TARGET target : entitiesToClear) {
                entitiesRemoved.put(target, Boolean.TRUE);
            }
            entitiesToClear.clear();
        }

        Map setToClear = entitiesAdded;
        if (setToClear != null) {
            setToClear.clear();
        }
    }

    @Override
    public boolean contains(Object object) {
        ensureEntities();
        return entities.contains(object);
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
        ensureEntities();
        return entities.containsAll(collection);
    }

    /**
     * @return An object for the given ID, or null if the object was already removed from its box
     * (and was not cached before).
     */
    @Override
    public TARGET get(int location) {
        ensureEntities();
        return entities.get(location);
    }

    @Override
    public int indexOf(Object object) {
        ensureEntities();
        return entities.indexOf(object);
    }

    @Override
    public boolean isEmpty() {
        ensureEntities();
        return entities.isEmpty();
    }

    @Override
    public Iterator<TARGET> iterator() {
        ensureEntities();
        return entities.iterator();
    }

    @Override
    public int lastIndexOf(Object object) {
        ensureEntities();
        return entities.lastIndexOf(object);
    }

    @Override
    public ListIterator<TARGET> listIterator() {
        ensureEntities();
        return entities.listIterator();
    }

    /**
     * The returned iterator does not track any potential calls to {@link Iterator#remove()}.
     * Thus these removes will NOT be synced to the target Box.
     */
    @Override
    public ListIterator<TARGET> listIterator(int location) {
        ensureEntities();
        return entities.listIterator(location);
    }

    @Override
    public synchronized TARGET remove(int location) {
        ensureEntitiesWithTrackingLists();
        TARGET removed = entities.remove(location);
        entitiesAdded.remove(removed);
        entitiesRemoved.put(removed, Boolean.TRUE);
        return removed;
    }

    @Override
    public synchronized boolean remove(Object object) {
        ensureEntitiesWithTrackingLists();
        boolean removed = entities.remove(object);
        if (removed) {
            entitiesAdded.remove(object);
            entitiesRemoved.put((TARGET) object, Boolean.TRUE);
        }
        return removed;
    }

    @Beta
    /** Removes an object by its entity ID. */
    public synchronized TARGET removeById(long id) {
        ensureEntities();
        int size = entities.size();
        IdGetter<TARGET> idGetter = relationInfo.targetInfo.getIdGetter();
        for (int i = 0; i < size; i++) {
            TARGET candidate = entities.get(i);
            if (idGetter.getId(candidate) == id) {
                TARGET removed = remove(i);
                if (removed != candidate) {
                    throw new IllegalStateException("Mismatch: " + removed + " vs. " + candidate);
                }
                return candidate;
            }

        }
        return null;
    }

    @Override
    public synchronized boolean removeAll(Collection<?> objects) {
        boolean changes = false;
        for (Object object : objects) {
            changes |= remove(object);
        }
        return changes;
    }

    @Override
    public synchronized boolean retainAll(Collection<?> objects) {
        ensureEntitiesWithTrackingLists();
        boolean changes = false;
        List<TARGET> toRemove = null;
        // Do not use Iterator with remove because not all List Types support it (e.g. CopyOnWriteArrayList)
        for (TARGET target : entities) {
            if (!objects.contains(target)) {
                if (toRemove == null) {
                    toRemove = new ArrayList<>();
                }
                toRemove.add(target);
                entitiesAdded.remove(target);
                entitiesRemoved.put((TARGET) target, Boolean.TRUE);
                changes = true;
            }
        }
        if (toRemove != null) {
            entities.removeAll(toRemove);
        }
        return changes;
    }

    @Override
    public synchronized TARGET set(int location, TARGET object) {
        ensureEntitiesWithTrackingLists();
        TARGET old = entities.set(location, object);
        entitiesAdded.remove(old);
        entitiesAdded.put(object, Boolean.TRUE);
        entitiesRemoved.remove(object);
        entitiesRemoved.put(old, Boolean.TRUE);
        return old;
    }

    @Override
    public int size() {
        ensureEntities();
        return entities.size();
    }

    /**
     * The returned sub list does not do any change tracking.
     * Thus any modifications to the sublist won't be synced to the target Box.
     */
    @Override
    public List<TARGET> subList(int start, int end) {
        ensureEntities();
        for (int i = start; i < end; i++) {
            get(i);
        }
        return entities.subList(start, end);
    }

    @Override
    public Object[] toArray() {
        ensureEntities();
        return entities.toArray();
    }

    @Override
    public <T> T[] toArray(T[] array) {
        ensureEntities();
        return entities.toArray(array);
    }

    /**
     * Resets the already loaded entities so they will be re-loaded on their next access.
     * This allows to sync with non-tracked changes (outside of this ToMany object).
     */
    public synchronized void reset() {
        entities = null;
        entitiesAdded = null;
        entitiesRemoved = null;
        entitiesToRemove = null;
        entitiesToPut = null;
    }

    public boolean isResolved() {
        return entities != null;
    }

    public int getAddCount() {
        Map<TARGET, Boolean> set = this.entitiesAdded;
        return set != null ? set.size() : 0;
    }

    public int getRemoveCount() {
        Map<TARGET, Boolean> set = this.entitiesRemoved;
        return set != null ? set.size() : 0;
    }

    /**
     * Sorts the list by the "natural" ObjectBox order for to-many list (by entity ID).
     * This will be the order when you get the entities fresh (e.g. initially or after calling {@link #reset()}).
     * Note that non persisted entities (ID is zero) will be put to the end as they are still to get an ID.
     */
    public void sortById() {
        ensureEntities();
        Collections.sort(entities, new Comparator<TARGET>() {

            IdGetter<TARGET> idGetter = relationInfo.targetInfo.getIdGetter();

            @Override
            public int compare(TARGET o1, TARGET o2) {
                long id1 = idGetter.getId(o1);
                long id2 = idGetter.getId(o2);
                if (id1 == 0)
                    id1 = Long.MAX_VALUE;
                if (id2 == 0)
                    id2 = Long.MAX_VALUE;
                long delta = id1 - id2;
                // because of long we cannot simply return delta
                if (delta < 0)
                    return -1;
                else if (delta > 0)
                    return 1;
                else
                    return 0;
            }
        });
    }

    /**
     * Applies (persists) tracked changes (added and removed entities) to the target box
     * and/or updates standalone relations.
     * Note that this is done automatically when you put the source entity of this to-many relation.
     * However, if only this to-many relation has changed, it is more efficient to call this method.
     *
     * @throws IllegalStateException If the source entity of this to-many relation was not previously persisted
     */
    public void applyChangesToDb() {
        long id = relationInfo.sourceInfo.getIdGetter().getId(entity);
        if (id == 0) {
            throw new IllegalStateException(
                    "The source entity was not yet persisted (no ID), use box.put() on it instead");
        }
        try {
            ensureBoxes();
        } catch (DbDetachedException e) {
            throw new IllegalStateException("The source entity was not yet persisted, use box.put() on it instead");
        }
        if (internalCheckApplyToDbRequired()) {
            // We need a TX because we use two writers and both must use same TX (without: unchecked, SIGSEGV)
            boxStore.runInTx(new Runnable() {
                @Override
                public void run() {
                    Cursor sourceCursor = InternalAccess.getActiveTxCursor(entityBox);
                    Cursor<TARGET> targetCursor = InternalAccess.getActiveTxCursor(targetBox);
                    internalApplyToDb(sourceCursor, targetCursor);
                }
            });
        }
    }

    /**
     * Returns true if at least one of the entities matches the given filter.
     * <p>
     * For use with {@link io.objectbox.query.QueryBuilder#filter(QueryFilter)} inside a {@link QueryFilter} to check
     * to-many relation entities.
     */
    @Beta
    public boolean hasA(QueryFilter<TARGET> filter) {
        ensureEntities();
        Object[] objects = entities.toArray();
        for (Object target : objects) {
            if (filter.keep((TARGET) target)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if all of the entities match the given filter. Returns false if the list is empty.
     * <p>
     * For use with {@link io.objectbox.query.QueryBuilder#filter(QueryFilter)} inside a {@link QueryFilter} to check
     * to-many relation entities.
     */
    @Beta
    public boolean hasAll(QueryFilter<TARGET> filter) {
        ensureEntities();
        Object[] objects = entities.toArray();
        if (objects.length == 0) {
            return false;
        }
        for (Object target : objects) {
            if (!filter.keep((TARGET) target)) {
                return false;
            }
        }
        return true;
    }

    @Beta
    /** Gets an object by its entity ID. */
    public TARGET getById(long id) {
        ensureEntities();
        Object[] objects = entities.toArray();
        IdGetter<TARGET> idGetter = relationInfo.targetInfo.getIdGetter();
        for (Object target : objects) {
            TARGET candidate = (TARGET) target;
            if (idGetter.getId(candidate) == id) {
                return candidate;
            }
        }
        return null;
    }

    @Beta
    /** Gets the index of the object with the given entity ID. */
    public int indexOfId(long id) {
        ensureEntities();
        Object[] objects = entities.toArray();
        IdGetter<TARGET> idGetter = relationInfo.targetInfo.getIdGetter();
        int index = 0;
        for (Object target : objects) {
            TARGET candidate = (TARGET) target;
            if (idGetter.getId(candidate) == id) {
                return index;
            }
            index++;
        }
        return -1;
    }

    /**
     * For internal use only; do not use in your app.
     * Called after relation source entity is put (so we have its ID).
     * Prepares data for {@link #internalApplyToDb(Cursor, Cursor)}
     */
    @Internal
    public boolean internalCheckApplyToDbRequired() {
        Map<TARGET, Boolean> setAdded = this.entitiesAdded;
        Map<TARGET, Boolean> setRemoved = this.entitiesRemoved;
        if ((setAdded == null || setAdded.isEmpty()) && (setRemoved == null || setRemoved.isEmpty())) {
            return false;
        }
        io.objectbox.internal.ToOneGetter backlinkToOneGetter = relationInfo.backlinkToOneGetter;
        long entityId = relationInfo.sourceInfo.getIdGetter().getId(entity);
        if (entityId == 0) {
            throw new IllegalStateException("Source entity has no ID (should have been put before)");
        }
        IdGetter<TARGET> idGetter = relationInfo.targetInfo.getIdGetter();
        boolean isStandaloneRelation = relationInfo.relationId != 0;
        synchronized (this) {
            if (entitiesToPut == null) {
                entitiesToPut = new ArrayList<>();
                entitiesToRemove = new ArrayList<>();
            }
            if (isStandaloneRelation) {
                // No prep here, all is done inside a single synchronized block in internalApplyToDb
                return !setAdded.isEmpty() || !setRemoved.isEmpty();
            } else {
                for (TARGET target : setAdded.keySet()) {
                    ToOne<Object> toOne = backlinkToOneGetter.getToOne(target);
                    if (toOne == null) {
                        throw new IllegalStateException("The ToOne property for " +
                                relationInfo.targetInfo.getEntityName() + "." + relationInfo.targetIdProperty.name +
                                " is null");
                    }
                    long toOneTargetId = toOne.getTargetId();
                    if (toOneTargetId != entityId) {
                        toOne.setTarget(entity);
                        entitiesToPut.add(target);
                    } else if (idGetter.getId(target) == 0) {
                        entitiesToPut.add(target);
                    }
                }
                setAdded.clear();

                for (TARGET target : setRemoved.keySet()) {
                    ToOne<Object> toOne = backlinkToOneGetter.getToOne(target);
                    long toOneTargetId = toOne.getTargetId();
                    if (toOneTargetId == entityId) {
                        toOne.setTarget(null);
                        if (removeFromTargetBox) {
                            entitiesToRemove.add(target);
                        } else {
                            entitiesToPut.add(target);
                        }
                    }
                }
                setRemoved.clear();
                return !entitiesToPut.isEmpty() || !entitiesToRemove.isEmpty();
            }
        }
    }

    /**
     * For internal use only; do not use in your app.
     * Convention: {@link #internalCheckApplyToDbRequired()} must be called before this call as it prepares .
     */
    @Internal
    public void internalApplyToDb(Cursor sourceCursor, Cursor<TARGET> targetCursor) {
        TARGET[] toRemove;
        TARGET[] toPut;
        TARGET[] addedStandalone = null;
        TARGET[] removedStandalone = null;

        boolean isStandaloneRelation = relationInfo.relationId != 0;
        IdGetter<TARGET> targetIdGetter = relationInfo.targetInfo.getIdGetter();
        synchronized (this) {
            if (isStandaloneRelation) {
                for (TARGET target : entitiesAdded.keySet()) {
                    if (targetIdGetter.getId(target) == 0) {
                        entitiesToPut.add(target);
                    }
                }
                if (removeFromTargetBox) {
                    entitiesToRemove.addAll(entitiesRemoved.keySet());
                }
                if (!entitiesAdded.isEmpty()) {
                    addedStandalone = (TARGET[]) entitiesAdded.keySet().toArray();
                    entitiesAdded.clear();
                }
                if (!entitiesRemoved.isEmpty()) {
                    removedStandalone = (TARGET[]) entitiesRemoved.keySet().toArray();
                    entitiesRemoved.clear();
                }
            }

            toRemove = entitiesToRemove.isEmpty() ? null : (TARGET[]) entitiesToRemove.toArray();
            entitiesToRemove.clear();
            toPut = entitiesToPut.isEmpty() ? null : (TARGET[]) entitiesToPut.toArray();
            entitiesToPut.clear();
        }

        if (toRemove != null) {
            for (TARGET target : toRemove) {
                long id = targetIdGetter.getId(target);
                targetCursor.deleteEntity(id);
            }
        }
        if (toPut != null) {
            for (TARGET target : toPut) {
                targetCursor.put(target);
            }
        }

        if (isStandaloneRelation) {
            long entityId = relationInfo.sourceInfo.getIdGetter().getId(entity);
            if (entityId == 0) {
                throw new IllegalStateException("Source entity has no ID (should have been put before)");
            }

            checkModifyStandaloneRelation(sourceCursor, entityId, removedStandalone, targetIdGetter, true);
            checkModifyStandaloneRelation(sourceCursor, entityId, addedStandalone, targetIdGetter, false);
        }
    }

    private void checkModifyStandaloneRelation(Cursor cursor, long sourceEntityId, TARGET[] targets,
                                               IdGetter<TARGET> targetIdGetter, boolean remove) {
        if (targets != null) {
            int length = targets.length;
            long[] targetIds = new long[length];
            for (int i = 0; i < length; i++) {
                long targetId = targetIdGetter.getId(targets[i]);
                if (targetId == 0) {
                    // Paranoia
                    throw new IllegalStateException("Target entity has no ID (should have been put before)");
                }
                targetIds[i] = targetId;
            }
            cursor.modifyRelations(relationInfo.relationId, sourceEntityId, targetIds, remove);
        }
    }

    /** For tests */
    Object getEntity() {
        return entity;
    }
}
