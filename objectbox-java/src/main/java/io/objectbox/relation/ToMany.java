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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import io.objectbox.Box;
import io.objectbox.BoxStore;
import io.objectbox.Cursor;
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

    /** Used as a set (value is always Boolean.TRUE). */
    private Map<TARGET, Boolean> entitiesAdded;

    private BoxStore boxStore;
    private Box entityBox;
    private volatile Box<TARGET> targetBox;

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
        return entities.add(object);
    }

    @Override
    public synchronized void add(int location, TARGET object) {
        ensureEntitiesWithModifications();
        entitiesAdded.put(object, Boolean.TRUE);
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
        }
    }

    @Override
    public synchronized boolean addAll(int index, Collection<? extends TARGET> objects) {
        putAllToAdded(objects);
        return entities.addAll(index, objects);
    }

    @Override
    public synchronized void clear() {
        List<TARGET> entitiesToClear = entities;
        if (entitiesToClear != null) {
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

    @Override
    public ListIterator<TARGET> listIterator(int location) {
        ensureEntities();
        return entities.listIterator(location);
    }

    @Override
    public TARGET remove(int location) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object object) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public TARGET set(int location, TARGET object) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int size() {
        ensureEntities();
        return entities.size();
    }

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

    @Internal
    public boolean internalRequiresPutTarget() {
        Map<TARGET, Boolean> set = this.entitiesAdded;
        return set != null && !set.isEmpty();
    }

    @Internal
    public void internalPutTarget(Cursor<TARGET> targetCursor) {
        List<TARGET> putCandidates;
        synchronized (this) {
            putCandidates = new ArrayList<>(entitiesAdded.keySet());
            entitiesAdded.clear();
        }
        ToOneGetter toOneGetter = relationInfo.toOneGetter;
        long entityId = relationInfo.sourceInfo.getIdGetter().getId(entity);
        IdGetter<TARGET> idGetter = relationInfo.targetInfo.getIdGetter();

        for (TARGET target : putCandidates) {
            ToOne<Object> toOne = toOneGetter.getToOne(target);
            long toOneTargetId = toOne.getTargetId();
            if (toOneTargetId != entityId) {
                toOne.setTargetId(entityId);
                targetCursor.put(target);
            } else if (idGetter.getId(target) == 0) {
                targetCursor.put(target);
            }
        }
    }

}
