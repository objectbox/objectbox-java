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
import java.util.List;
import java.util.ListIterator;

import io.objectbox.Box;
import io.objectbox.BoxStore;
import io.objectbox.exception.DbDetachedException;
import io.objectbox.internal.ReflectionCache;

/**
 * @param <TARGET> Object type (entity).
 */
public class ToMany<TARGET> implements List<TARGET> {

    private final Object entity;
    private final RelationInfo<TARGET> relationInfo;

    private List<TARGET> entities;
    private List<TARGET> entitiesAdded;

    private BoxStore boxStore;
    private Box entityBox;
    private volatile Box<TARGET> targetBox;

    public ToMany(Object sourceEntity, RelationInfo<TARGET> relationInfo) {
        this.entity = sourceEntity;
        this.relationInfo = relationInfo;
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

    private void checkGetEntities() {
        if (entities == null) {
            long id = relationInfo.sourceInfo.getIdGetter().getId(entity);
            if (id == 0) {
                // Not yet persisted entity
                synchronized (this) {
                    if (entities == null) {
                        entities = new ArrayList<>();
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
        throw new UnsupportedOperationException();
    }

    @Override
    public void add(int location, TARGET object) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(Collection<? extends TARGET> arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(int arg0, Collection<? extends TARGET> arg1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean contains(Object object) {
        checkGetEntities();
        return entities.contains(object);
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
        checkGetEntities();
        return entities.containsAll(collection);
    }

    /**
     * @return An object for the given ID, or null if the object was already removed from its box
     * (and was not cached before).
     */
    @Override
    public TARGET get(int location) {
        checkGetEntities();
        return entities.get(location);
    }

    @Override
    public int indexOf(Object object) {
        checkGetEntities();
        return entities.indexOf(object);
    }

    @Override
    public boolean isEmpty() {
        checkGetEntities();
        return entities.isEmpty();
    }

    @Override
    public Iterator<TARGET> iterator() {
        checkGetEntities();
        return entities.iterator();
    }

    @Override
    public int lastIndexOf(Object object) {
        checkGetEntities();
        return entities.lastIndexOf(object);
    }

    @Override
    public ListIterator<TARGET> listIterator() {
        checkGetEntities();
        return entities.listIterator();
    }

    @Override
    public ListIterator<TARGET> listIterator(int location) {
        checkGetEntities();
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
        checkGetEntities();
        return entities.size();
    }

    @Override
    public List<TARGET> subList(int start, int end) {
        checkGetEntities();
        for (int i = start; i < end; i++) {
            get(i);
        }
        return entities.subList(start, end);
    }

    @Override
    public Object[] toArray() {
        checkGetEntities();
        return entities.toArray();
    }

    @Override
    public <T> T[] toArray(T[] array) {
        checkGetEntities();
        return entities.toArray(array);
    }

    public synchronized void reset() {
        entities = null;
        entitiesAdded = null;
    }

}
