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
import io.objectbox.annotation.apihint.Experimental;
import io.objectbox.annotation.apihint.Internal;
import io.objectbox.exception.DbDetachedException;
import io.objectbox.internal.IdGetter;
import io.objectbox.internal.ReflectionCache;
import io.objectbox.relation.ListFactory.CopyOnWriteArrayListFactory;

/**
 * A List representing a to-many relation.
 * Is thread-safe by default (using the default {@link java.util.concurrent.CopyOnWriteArrayList}).
 *
 * @param <TARGET> Object type (entity).
 */
public class ToMany<TARGET> implements List<TARGET> {

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

    private BoxStore boxStore;
    private Box entityBox;
    private volatile Box<TARGET> targetBox;
    private boolean removeFromTargetBox;

    public ToMany(Object sourceEntity, RelationInfo<TARGET> relationInfo) {
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

    private void ensureEntitiesWithModifications() {
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
                List<TARGET> newEntities = targetBox.getBacklinkEntities(relationInfo.targetInfo.getEntityId(),
                        relationInfo.targetIdProperty, id);
                synchronized (this) {
                    if (entities == null) {
                        entities = newEntities;
                    }
                }
            }
        }
    }

    @Override
    public synchronized boolean add(TARGET object) {
        ensureEntitiesWithModifications();
        entitiesAdded.put(object, Boolean.TRUE);
        entitiesRemoved.remove(object);
        return entities.add(object);
    }

    @Override
    public synchronized void add(int location, TARGET object) {
        ensureEntitiesWithModifications();
        entitiesAdded.put(object, Boolean.TRUE);
        entitiesRemoved.remove(object);
        entities.add(location, object);
    }

    @Override
    public synchronized boolean addAll(Collection<? extends TARGET> objects) {
        putAllToAdded(objects);
        return entities.addAll(objects);
    }

    private synchronized void putAllToAdded(Collection<? extends TARGET> objects) {
        ensureEntitiesWithModifications();
        for (TARGET object : objects) {
            entitiesAdded.put(object, Boolean.TRUE);
            entitiesRemoved.remove(object);
        }
    }

    @Override
    public synchronized boolean addAll(int index, Collection<? extends TARGET> objects) {
        putAllToAdded(objects);
        return entities.addAll(index, objects);
    }

    @Override
    public synchronized void clear() {
        ensureEntitiesWithModifications();
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
        ensureEntitiesWithModifications();
        TARGET removed = entities.remove(location);
        entitiesAdded.remove(removed);
        entitiesRemoved.put(removed, Boolean.TRUE);
        return removed;
    }

    @Override
    public synchronized boolean remove(Object object) {
        ensureEntitiesWithModifications();
        boolean removed = entities.remove(object);
        if (removed) {
            entitiesAdded.remove(object);
            entitiesRemoved.put((TARGET) object, Boolean.TRUE);
        }
        return removed;
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
        ensureEntitiesWithModifications();
        boolean changes = false;
        Iterator<TARGET> iterator = entities.iterator();
        while (iterator.hasNext()) {
            TARGET target = iterator.next();
            if (!objects.contains(target)) {
                iterator.remove();
                entitiesAdded.remove(target);
                entitiesRemoved.put((TARGET) target, Boolean.TRUE);
                changes = true;
            }
        }
        return changes;
    }

    @Override
    public synchronized TARGET set(int location, TARGET object) {
        ensureEntitiesWithModifications();
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
                if (id1 == 0) id1 = Long.MAX_VALUE;
                if (id2 == 0) id2 = Long.MAX_VALUE;
                long delta = id1 - id2;
                // because of long we cannot simply return delta
                if (delta < 0) return -1;
                else if (delta > 0) return 1;
                else return 0;
            }
        });
    }

    /**
     * Syncs (persists) tracked changes (added and removed entities) to the target box.
     * Note that this is done automatically when you put the source entity of this to-many relation.
     * However, if only this to-many relation has changed, it is more efficient to call this method.
     *
     * @throws IllegalStateException If the source entity of this to-many relation was not previously persisted
     */
    public void syncToTargetBox() {
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
        if (internalRequiresPutTarget()) {
            Cursor<TARGET> writer = InternalAccess.getWriter(targetBox);
            try {
                internalPutTarget(writer);
                InternalAccess.commitWriter(targetBox, writer);
            } finally {
                InternalAccess.releaseWriter(targetBox, writer);
            }
        }
    }

    /** Called after relation source entity is put (so we have its ID). */
    @Internal
    public boolean internalRequiresPutTarget() {
        Map<TARGET, Boolean> setAdded = this.entitiesAdded;
        Map<TARGET, Boolean> setRemoved = this.entitiesRemoved;
        if ((setAdded == null || setAdded.isEmpty()) && (setRemoved == null || setRemoved.isEmpty())) {
            return false;
        }
        ToOneGetter toOneGetter = relationInfo.toOneGetter;
        long entityId = relationInfo.sourceInfo.getIdGetter().getId(entity);
        if (entityId == 0) {
            throw new IllegalStateException("Source entity has no ID (potential internal error)");
        }
        IdGetter<TARGET> idGetter = relationInfo.targetInfo.getIdGetter();
        synchronized (this) {
            if (entitiesToPut == null) {
                entitiesToPut = new ArrayList<>();
                entitiesToRemove = new ArrayList<>();
            }
            for (TARGET target : setAdded.keySet()) {
                ToOne<Object> toOne = toOneGetter.getToOne(target);
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
                ToOne<Object> toOne = toOneGetter.getToOne(target);
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

    @Internal
    public void internalPutTarget(Cursor<TARGET> targetCursor) {
        TARGET[] toRemove;
        TARGET[] toPut;
        synchronized (this) {
            toRemove = entitiesToRemove.isEmpty() ? null : (TARGET[]) entitiesToRemove.toArray();
            entitiesToRemove.clear();
            toPut = entitiesToPut.isEmpty() ? null : (TARGET[]) entitiesToPut.toArray();
            entitiesToPut.clear();
        }

        if (toRemove != null) {
            IdGetter<TARGET> targetIdGetter = relationInfo.targetInfo.getIdGetter();
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
    }

}
