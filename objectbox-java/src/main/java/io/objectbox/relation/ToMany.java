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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import io.objectbox.Box;
import io.objectbox.BoxStore;
import io.objectbox.annotation.apihint.Experimental;
import io.objectbox.exception.DbDetachedException;
import io.objectbox.internal.ReflectionCache;
import io.objectbox.relation.ListFactory.CopyOnWriteArrayListFactory;

/**
 * A List representing a to-many relation.
 * Not thread-safe (unless strictly read-only); for multithreaded use consider wrapping this list with
 * {@link java.util.Collections#synchronizedList(List)}.
 *
 * @param <TARGET> Object type (entity).
 */
// TODO investigate if we can make it thread-safe if required (using ListFactory etc.)
public class ToMany<TARGET> implements List<TARGET> {

    private final Object entity;
    private final RelationInfo<TARGET> relationInfo;

    private ListFactory listFactory = new CopyOnWriteArrayListFactory();
    private List<TARGET> entities;
    private List<TARGET> entitiesAdded;

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
                    entitiesAdded = listFactory.createList();
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
                        entities = listFactory.createList();
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
    public boolean add(TARGET object) {
        ensureEntitiesWithModifications();
        entitiesAdded.add(object);
        return entities.add(object);
    }

    @Override
    public void add(int location, TARGET object) {
        ensureEntitiesWithModifications();
        entitiesAdded.add(object);
        entities.add(location, object);
    }

    @Override
    public boolean addAll(Collection<? extends TARGET> objects) {
        ensureEntitiesWithModifications();
        entitiesAdded.addAll(objects);
        return entities.addAll(objects);
    }

    @Override
    public boolean addAll(int index, Collection<? extends TARGET> objects) {
        ensureEntitiesWithModifications();
        entitiesAdded.addAll(objects);
        return entities.addAll(objects);
    }

    @Override
    public void clear() {
        List<TARGET> entitiesToClear = entities;
        if(entitiesToClear != null) {
            entitiesToClear.clear();
        }

        entitiesToClear = entitiesAdded;
        if(entitiesToClear != null) {
            entitiesToClear.clear();
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

}
