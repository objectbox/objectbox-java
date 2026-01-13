/*
 * Copyright 2017 ObjectBox Ltd.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import javax.annotation.Nullable;

import io.objectbox.Box;
import io.objectbox.exception.DbException;

/**
 * A thread-safe, unmodifiable {@link List} that gets Objects from their Box not until they are accessed.
 * Internally the list is backed by an array of Object IDs.
 * <p>
 * If the list is set to not cache retrieved Objects, each operation will get the latest version of an Object
 * from its Box. However, in this mode only a limited set of {@link List} operations,
 * like get or iterator are supported.
 * <p>
 * If the list is set to cache retrieved Objects, operations will return a previously fetched version of an Object,
 * which might not equal the latest version in its Box. However, in this mode almost all {@link List}
 * operations are supported. Note that operations that require the whole list, like contains, will fetch all
 * Objects in this list from the Box at once.
 * <p>
 * Note: as Objects are fetched on demand, this list returns a null Object if the Object was removed from its Box
 * after this list was created.
 *
 * @param <E> Object type (entity).
 */
/*
 Threading note: locking is tailored to ArrayList assuming that concurrent positional gets/sets are OK.
 To enable this, the internal ArrayList is prepopulated with null.
*/
public class LazyList<E> implements List<E> {
    protected class LazyIterator implements ListIterator<E> {
        private int index;

        public LazyIterator(int startLocation) {
            index = startLocation;
        }

        @Override
        public void add(E object) {
            throw new UnsupportedOperationException();
        }

        /** FIXME: before hasPrevious(), next() must be called. */
        @Override
        public boolean hasPrevious() {
            return index > 0;
        }

        @Override
        public int nextIndex() {
            return index;
        }

        /** FIXME: before previous(), next() must be called. */
        @Override
        public E previous() {
            if (index <= 0) {
                throw new NoSuchElementException();
            }
            index--;
            return get(index);
        }

        @Override
        public int previousIndex() {
            return index - 1;
        }

        @Override
        public void set(E object) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean hasNext() {
            return index < size;
        }

        @Override
        public E next() {
            if (index >= size) {
                throw new NoSuchElementException();
            }
            E entity = get(index);
            index++;
            return entity;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    private final Box<E> box;
    private final long[] objectIds;
    private final List<E> entities;

    // Accessed by iterator: avoid private
    final int size;

    private volatile int loadedCount;

    LazyList(Box<E> box, long[] objectIds, boolean cacheEntities) {
        if (box == null || objectIds == null) {
            throw new NullPointerException("Illegal null parameters passed");
        }
        this.box = box;
        this.objectIds = objectIds;
        size = objectIds.length;
        if (cacheEntities) {
            entities = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                entities.add(null);
            }
        } else {
            entities = null;
        }
    }

    /** Loads the remaining entities (if any) that were not loaded before. Applies to cached lazy lists only. */
    public void loadRemaining() {
        if (loadedCount != size) {
            checkCached();
            // use single reader only for efficiency
            box.getStore().runInReadTx(() -> {
                for (int i = 0; i < size; i++) {
                    //noinspection ResultOfMethodCallIgnored
                    get(i);
                }
            });
        }
    }

    protected void checkCached() {
        if (entities == null) {
            throw new DbException("This operation only works with cached lazy lists");
        }
    }

    /** Like get but does not load the entity if it was not loaded before. */
    public E peek(int location) {
        if (entities != null) {
            return entities.get(location);
        } else {
            return null;
        }
    }

    public int getLoadedCount() {
        return loadedCount;
    }

    public boolean isLoadedCompletely() {
        return loadedCount == size;
    }

    @Override
    public boolean add(E object) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void add(int location, E object) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(Collection<? extends E> arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(int arg0, Collection<? extends E> arg1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean contains(Object object) {
        loadRemaining();
        return entities.contains(object);
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
        loadRemaining();
        return entities.containsAll(collection);
    }

    /**
     * Gets and returns the Object at the specified position in this list from its Box. Returns null if the
     * Object was removed from its Box. If this list is caching retrieved Objects, returns the previously
     * fetched version.
     */
    @Nullable
    @Override
    public E get(int location) {
        if (location < 0 || location > size) {
            throw new IndexOutOfBoundsException("Illegal cursor location " + location);
        }
        if (entities != null) {
            E entity = entities.get(location);
            if (entity == null) {
                // Do DB action outside of synchronized and check later if we use the new entity.
                E newEntity = box.get(objectIds[location]);
                synchronized (this) {
                    // Check again to ensure that always the same entity is returned once cached
                    entity = entities.get(location);
                    if (entity == null) {
                        entity = newEntity;
                        entities.set(location, newEntity);
                        // Ignore FindBugs: increment of volatile is fine here because we use synchronized
                        loadedCount++;
                    }
                }
            }
            return entity;
        } else {
            synchronized (this) {
                return box.get(objectIds[location]);
            }
        }
    }

    @Override
    public int indexOf(Object object) {
        loadRemaining();
        return entities.indexOf(object);
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public Iterator<E> iterator() {
        return new LazyIterator(0);
    }

    @Override
    public int lastIndexOf(Object object) {
        loadRemaining();
        return entities.lastIndexOf(object);
    }

    @Override
    public ListIterator<E> listIterator() {
        return new LazyIterator(0);
    }

    @Override
    public ListIterator<E> listIterator(int location) {
        if (location < 0 || location > size) {
            throw new IndexOutOfBoundsException("Index: " + location + ", Size: " + size);
        }
        return new LazyIterator(location);
    }

    @Override
    public E remove(int location) {
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
    public E set(int location, E object) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public List<E> subList(int start, int end) {
        checkCached();
        for (int i = start; i < end; i++) {
            get(i);
        }
        return entities.subList(start, end);
    }

    @Override
    public Object[] toArray() {
        loadRemaining();
        return entities.toArray();
    }

    @Override
    public <T> T[] toArray(T[] array) {
        loadRemaining();
        return entities.toArray(array);
    }

}
