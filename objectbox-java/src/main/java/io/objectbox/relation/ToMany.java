/*
 * Copyright 2017-2024 ObjectBox Ltd.
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
package io.objectbox.relation;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import io.objectbox.Box;
import io.objectbox.BoxStore;
import io.objectbox.Cursor;
import io.objectbox.InternalAccess;
import io.objectbox.annotation.Backlink;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.apihint.Beta;
import io.objectbox.annotation.apihint.Experimental;
import io.objectbox.annotation.apihint.Internal;
import io.objectbox.exception.DbDetachedException;
import io.objectbox.internal.IdGetter;
import io.objectbox.internal.ReflectionCache;
import io.objectbox.internal.ToManyGetter;
import io.objectbox.internal.ToOneGetter;
import io.objectbox.query.QueryBuilder;
import io.objectbox.query.QueryFilter;
import io.objectbox.relation.ListFactory.CopyOnWriteArrayListFactory;

import static java.lang.Boolean.TRUE;

/**
 * A to-many relation of an entity that references multiple objects of a {@link TARGET} entity.
 * <p>
 * Example:
 * <pre>{@code
 * // Java
 * @Entity
 * public class Student {
 *     private ToMany<Teacher> teachers;
 * }
 *
 * // Kotlin
 * @Entity
 * data class Student() {
 *     lateinit var teachers: ToMany<Teacher>
 * }
 * }</pre>
 * <p>
 * Implements the {@link List} interface and uses lazy initialization. The target objects are only read from the
 * database when the list is first accessed.
 * <p>
 * The required database query runs on the calling thread, so avoid accessing ToMany from a UI or main thread. To get the
 * latest data {@link Box#get} the object with the ToMany again or use {@link #reset()} before accessing the list again.
 * It is possible to preload the list when running a query using {@link QueryBuilder#eager}.
 * <p>
 * Tracks when target objects are added and removed. Common usage:
 * <ul>
 * <li>{@link #add(Object)} to add target objects to the relation.
 * <li>{@link #remove(Object)} to remove target objects from the relation.
 * <li>{@link #remove(int)} to remove target objects at a specific index.
 * </ul>
 * <p>
 * To apply (persist) the changes to the database, call {@link #applyChangesToDb()} or put the object with the ToMany.
 * For important details, see the notes about relations of {@link Box#put(Object)}.
 * <pre>{@code
 * // Example 1: add target objects to a relation
 * student.getTeachers().add(teacher1);
 * student.getTeachers().add(teacher2);
 * store.boxFor(Student.class).put(student);
 *
 * // Example 2: remove a target object from the relation
 * student.getTeachers().remove(index);
 * student.getTeachers().applyChangesToDb();
 * // or store.boxFor(Student.class).put(student);
 * }</pre>
 * <p>
 * In the database, the target objects are referenced by their IDs, which are persisted as part of the relation of the
 * object with the ToMany.
 * <p>
 * ToMany is thread-safe by default (may not be the case if {@link #setListFactory(ListFactory)} is used).
 * <p>
 * To get all objects with a ToMany that reference a target object, see {@link Backlink}.
 *
 * @param <TARGET> target object type ({@link Entity @Entity} class).
 */
public class ToMany<TARGET> implements List<TARGET>, Serializable {
    private static final long serialVersionUID = 2367317778240689006L;
    private final static Integer ONE = Integer.valueOf(1);

    private final Object entity;
    private final RelationInfo<Object, TARGET> relationInfo;

    private volatile ListFactory listFactory;
    private List<TARGET> entities;

    /** Counts of all entities in the list ({@link #entities}). */
    private Map<TARGET, Integer> entityCounts;

    /** Entities added since last put/sync. Map is used as a set (value is always Boolean.TRUE). */
    private volatile Map<TARGET, Boolean> entitiesAdded;

    /** Entities removed since last put/sync. Map is used as a set (value is always Boolean.TRUE). */
    private Map<TARGET, Boolean> entitiesRemoved;

    List<TARGET> entitiesToPut;
    List<TARGET> entitiesToRemoveFromDb;

    transient private BoxStore boxStore;
    transient private Box<Object> entityBox;
    transient private volatile Box<TARGET> targetBox;
    transient private boolean removeFromTargetBox;
    transient private Comparator<TARGET> comparator;

    @SuppressWarnings("unchecked") // RelationInfo cast: ? is at least Object.
    public ToMany(Object sourceEntity, RelationInfo<?, TARGET> relationInfo) {
        //noinspection ConstantConditions Annotation does not enforce non-null.
        if (sourceEntity == null) {
            throw new IllegalArgumentException("No source entity given (null)");
        }
        //noinspection ConstantConditions Annotation does not enforce non-null.
        if (relationInfo == null) {
            throw new IllegalArgumentException("No relation info given (null)");
        }
        this.entity = sourceEntity;
        this.relationInfo = (RelationInfo<Object, TARGET>) relationInfo;
    }

    /** Currently only used for non-persisted entities (id == 0). */
    @Experimental
    public void setListFactory(ListFactory listFactory) {
        //noinspection ConstantConditions Annotation does not enforce non-null.
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
        ListFactory result = listFactory;
        if (result == null) {
            synchronized (this) {
                result = listFactory;
                if (result == null) {
                    listFactory = result = new CopyOnWriteArrayListFactory();
                }
            }
        }
        return result;
    }

    private void ensureBoxes() {
        if (targetBox == null) {
            Field boxStoreField = ReflectionCache.getInstance().getField(entity.getClass(), "__boxStore");
            try {
                boxStore = (BoxStore) boxStoreField.get(entity);
                if (boxStore == null) {
                    throw new DbDetachedException("Cannot resolve relation for detached objects, " +
                            "call box.attach(object) beforehand.");
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
                    entityCounts = new HashMap<>();
                    for (TARGET object : entities) {
                        Integer old = entityCounts.put(object, ONE);
                        if (old != null) {
                            entityCounts.put(object, old + 1);
                        }
                    }
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
                    newEntities = targetBox.internalGetRelationEntities(sourceEntityId, relationId, id, false);
                } else {
                    if (relationInfo.targetIdProperty != null) {
                        // Backlink from ToOne
                        newEntities = targetBox.internalGetBacklinkEntities(relationInfo.targetInfo.getEntityId(),
                                relationInfo.targetIdProperty, id);
                    } else {
                        // Backlink from ToMany
                        newEntities = targetBox.internalGetRelationEntities(relationInfo.targetInfo.getEntityId(),
                                relationInfo.targetRelationId, id, true);
                    }
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

    /**
     * Prepares to add the given target object to this relation.
     * <p>
     * To apply changes, call {@link #applyChangesToDb()} or put the object with the ToMany. For important details, see
     * the notes about relations of {@link Box#put(Object)}.
     */
    @Override
    public synchronized boolean add(TARGET object) {
        trackAdd(object);
        return entities.add(object);
    }

    /** Must be called from a synchronized method */
    private void trackAdd(TARGET object) {
        ensureEntitiesWithTrackingLists();
        Integer old = entityCounts.put(object, ONE);
        if (old != null) {
            entityCounts.put(object, old + 1);
        }
        entitiesAdded.put(object, TRUE);
        entitiesRemoved.remove(object);
    }

    /** Must be called from a synchronized method */
    private void trackAdd(Collection<? extends TARGET> objects) {
        ensureEntitiesWithTrackingLists();
        for (TARGET object : objects) {
            trackAdd(object);
        }
    }

    /** Must be called from a synchronized method */
    private void trackRemove(TARGET object) {
        ensureEntitiesWithTrackingLists();
        Integer count = entityCounts.remove(object);
        if (count != null) {
            if (count == 1) {
                entityCounts.remove(object);
                entitiesAdded.remove(object);

                entitiesRemoved.put(object, TRUE);
            } else if (count > 1) {
                entityCounts.put(object, count - 1);
            } else {
                throw new IllegalStateException("Illegal count: " + count);
            }
        }
    }

    /** See {@link #add(Object)} for general comments. */
    @Override
    public synchronized void add(int location, TARGET object) {
        trackAdd(object);
        entities.add(location, object);
    }

    /** See {@link #add(Object)} for general comments. */
    @Override
    public synchronized boolean addAll(Collection<? extends TARGET> objects) {
        trackAdd(objects);
        return entities.addAll(objects);
    }

    /** See {@link #add(Object)} for general comments. */
    @Override
    public synchronized boolean addAll(int index, Collection<? extends TARGET> objects) {
        trackAdd(objects);
        return entities.addAll(index, objects);
    }

    @Override
    public synchronized void clear() {
        ensureEntitiesWithTrackingLists();
        List<TARGET> entitiesToClear = entities;
        if (entitiesToClear != null) {
            for (TARGET target : entitiesToClear) {
                entitiesRemoved.put(target, TRUE);
            }
            entitiesToClear.clear();
        }

        Map<TARGET, Boolean> setToClear = entitiesAdded;
        if (setToClear != null) {
            setToClear.clear();
        }

        Map<TARGET, Integer> entityCountsToClear = this.entityCounts;
        if (entityCountsToClear != null) {
            entityCountsToClear.clear();
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
     * Gets the target object at the given index.
     * <p>
     * {@link ToMany} uses lazy initialization, so on first access this will read the target objects from the database.
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
    @Nonnull
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
    @Nonnull
    public ListIterator<TARGET> listIterator() {
        ensureEntities();
        return entities.listIterator();
    }

    /**
     * The returned iterator does not track any potential calls to {@link Iterator#remove()}.
     * Thus these removes will NOT be synced to the target Box.
     */
    @Override
    @Nonnull
    public ListIterator<TARGET> listIterator(int location) {
        ensureEntities();
        return entities.listIterator(location);
    }

    /**
     * Like {@link #remove(Object)}, but using the location of the target object.
     */
    @Override
    public synchronized TARGET remove(int location) {
        ensureEntitiesWithTrackingLists();
        TARGET removed = entities.remove(location);
        trackRemove(removed);
        return removed;
    }

    /**
     * Prepares to remove the target object from this relation.
     * <p>
     * To apply changes, call {@link #applyChangesToDb()} or put the object with the ToMany. For important details, see
     * the notes about relations of {@link Box#put(Object)}.
     */
    @SuppressWarnings("unchecked") // Cast to TARGET: If removed, must be of type TARGET.
    @Override
    public synchronized boolean remove(Object object) {
        ensureEntitiesWithTrackingLists();
        boolean removed = entities.remove(object);
        if (removed) {
            trackRemove((TARGET) object);
        }
        return removed;
    }

    /**
     * Like {@link #remove(Object)}, but using just the ID of the target object.
     */
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
                changes = true;
            }
        }
        if (toRemove != null) {
            removeAll(toRemove);
        }
        return changes;
    }

    @Override
    public synchronized TARGET set(int location, TARGET object) {
        ensureEntitiesWithTrackingLists();
        TARGET old = entities.set(location, object);
        trackRemove(old);
        trackAdd(object);
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
    @Nonnull
    @Override
    public List<TARGET> subList(int start, int end) {
        ensureEntities();
        return entities.subList(start, end);
    }

    @Override
    @Nonnull
    public Object[] toArray() {
        ensureEntities();
        return entities.toArray();
    }

    @Override
    @Nonnull
    public <T> T[] toArray(T[] array) {
        ensureEntities();
        //noinspection SuspiciousToArrayCall Caller must pass T that is supertype of TARGET.
        return entities.toArray(array);
    }

    /**
     * Resets the already loaded (cached) objects of this list, so they will be re-loaded when accessing this list
     * again.
     * <p>
     * Use this to sync with changes to this relation or target objects made outside of this ToMany.
     */
    public synchronized void reset() {
        entities = null;
        entitiesAdded = null;
        entitiesRemoved = null;
        entitiesToRemoveFromDb = null;
        entitiesToPut = null;
        entityCounts = null;
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
     * Sorts the list by the "natural" ObjectBox order for to-many list (by object ID).
     * This will be the order when you get the objects fresh (e.g. initially or after calling {@link #reset()}).
     * Note that non persisted objects (ID is zero) will be put to the end as they are still to get an ID.
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
     * Saves changes (added and removed objects) made to this relation to the database. For some important details, see
     * the notes about relations of {@link Box#put(Object)}.
     * <p>
     * Note that this is called already when the object that contains this ToMany is put. However, if only this ToMany
     * has changed, it is more efficient to just use this method.
     *
     * @throws IllegalStateException If the object that contains this ToMany has no ID assigned (it must have been put
     * before).
     */
    public void applyChangesToDb() {
        long id = relationInfo.sourceInfo.getIdGetter().getId(entity);
        if (id == 0) {
            throw new IllegalStateException(
                    "The object with the ToMany was not yet persisted (no ID), use box.put() on it instead");
        }
        try {
            ensureBoxes();
        } catch (DbDetachedException e) {
            throw new IllegalStateException("The object with the ToMany was not yet persisted, use box.put() on it instead");
        }
        if (internalCheckApplyToDbRequired()) {
            // We need a TX because we use two writers and both must use same TX (without: unchecked, SIGSEGV)
            boxStore.runInTx(() -> {
                Cursor<Object> sourceCursor = InternalAccess.getActiveTxCursor(entityBox);
                Cursor<TARGET> targetCursor = InternalAccess.getActiveTxCursor(targetBox);
                internalApplyToDb(sourceCursor, targetCursor);
            });
        }
    }

    /**
     * Returns true if at least one of the target objects matches the given filter.
     * <p>
     * For use with {@link QueryBuilder#filter(QueryFilter)} inside a {@link QueryFilter} to check
     * to-many relation objects.
     */
    @Beta
    public boolean hasA(QueryFilter<TARGET> filter) {
        @SuppressWarnings("unchecked") // Can't toArray(new TARGET[0]).
        TARGET[] objects = (TARGET[]) toArray();
        for (TARGET target : objects) {
            if (filter.keep(target)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if all of the target objects match the given filter. Returns false if the list is empty.
     * <p>
     * For use with {@link QueryBuilder#filter(QueryFilter)} inside a {@link QueryFilter} to check
     * to-many relation objects.
     */
    @Beta
    public boolean hasAll(QueryFilter<TARGET> filter) {
        @SuppressWarnings("unchecked") // Can't toArray(new TARGET[0]).
        TARGET[] objects = (TARGET[]) toArray();
        if (objects.length == 0) {
            return false;
        }
        for (TARGET target : objects) {
            if (!filter.keep(target)) {
                return false;
            }
        }
        return true;
    }

    /** Gets an object by its ID. */
    @Beta
    public TARGET getById(long id) {
        ensureEntities();
        @SuppressWarnings("unchecked") // Can't toArray(new TARGET[0]).
        TARGET[] objects = (TARGET[]) entities.toArray();
        IdGetter<TARGET> idGetter = relationInfo.targetInfo.getIdGetter();
        for (TARGET target : objects) {
            if (idGetter.getId(target) == id) {
                return target;
            }
        }
        return null;
    }

    /** Gets the index of the object with the given ID. */
    @Beta
    public int indexOfId(long id) {
        ensureEntities();
        @SuppressWarnings("unchecked") // Can't toArray(new TARGET[0]).
        TARGET[] objects = (TARGET[]) entities.toArray();
        IdGetter<TARGET> idGetter = relationInfo.targetInfo.getIdGetter();
        int index = 0;
        for (TARGET target : objects) {
            if (idGetter.getId(target) == id) {
                return index;
            }
            index++;
        }
        return -1;
    }

    /**
     * Returns true if there are pending changes for the DB.
     * Changes will be automatically persisted once the object with the ToMany is put, or an explicit call to
     * {@link #applyChangesToDb()} is made.
     */
    public boolean hasPendingDbChanges() {
        Map<TARGET, Boolean> setAdded = this.entitiesAdded;
        if (setAdded != null && !setAdded.isEmpty()) {
            return true;
        } else {
            Map<TARGET, Boolean> setRemoved = this.entitiesRemoved;
            return setRemoved != null && !setRemoved.isEmpty();
        }
    }

    /**
     * For internal use only; do not use in your app.
     * Called after relation source object is put (so we have its ID).
     * Prepares data for {@link #internalApplyToDb(Cursor, Cursor)}
     */
    @Internal
    public boolean internalCheckApplyToDbRequired() {
        if (!hasPendingDbChanges()) {
            return false;
        }

        synchronized (this) {
            if (entitiesToPut == null) {
                entitiesToPut = new ArrayList<>();
                entitiesToRemoveFromDb = new ArrayList<>();
            }
        }

        if (relationInfo.relationId != 0) {
            // No preparation for standalone relations needed:
            // everything is done inside a single synchronized block in internalApplyToDb
            return true;
        } else {
            // Relation based on Backlink
            long entityId = relationInfo.sourceInfo.getIdGetter().getId(entity);
            if (entityId == 0) {
                throw new IllegalStateException("Object with the ToMany has no ID (should have been put before)");
            }
            IdGetter<TARGET> idGetter = relationInfo.targetInfo.getIdGetter();
            Map<TARGET, Boolean> setAdded = this.entitiesAdded;
            Map<TARGET, Boolean> setRemoved = this.entitiesRemoved;

            if (relationInfo.targetRelationId != 0) {
                return prepareToManyBacklinkEntitiesForDb(entityId, idGetter, setAdded, setRemoved);
            } else {
                return prepareToOneBacklinkEntitiesForDb(entityId, idGetter, setAdded, setRemoved);
            }
        }
    }

    /**
     * Modifies the {@link Backlink linked} ToMany relation of added or removed target objects and schedules put by
     * {@link #internalApplyToDb} for them.
     * <p>
     * If {@link #setRemoveFromTargetBox} is true, removed target objects are scheduled for removal instead of just
     * updating their ToMany relation.
     * <p>
     * If target objects are new, schedules a put if they were added, but never if they were removed from this relation.
     *
     * @return Whether there are any target objects to put or remove.
     */
    private boolean prepareToManyBacklinkEntitiesForDb(long entityId, IdGetter<TARGET> idGetter,
                                                       @Nullable Map<TARGET, Boolean> setAdded, @Nullable Map<TARGET, Boolean> setRemoved) {
        ToManyGetter<TARGET, Object> backlinkToManyGetter = relationInfo.backlinkToManyGetter;

        synchronized (this) {
            if (setAdded != null && !setAdded.isEmpty()) {
                for (TARGET target : setAdded.keySet()) {
                    ToMany<Object> toMany = (ToMany<Object>) backlinkToManyGetter.getToMany(target);
                    if (toMany == null) {
                        throw new IllegalStateException("The ToMany property for " +
                                relationInfo.targetInfo.getEntityName() + " is null");
                    }
                    if (toMany.getById(entityId) == null) {
                        // not yet in target relation
                        toMany.add(entity);
                        entitiesToPut.add(target);
                    } else if (idGetter.getId(target) == 0) {
                        // in target relation, but target not persisted, yet
                        entitiesToPut.add(target);
                    }
                }
                setAdded.clear();
            }

            if (setRemoved != null) {
                for (TARGET target : setRemoved.keySet()) {
                    ToMany<Object> toMany = (ToMany<Object>) backlinkToManyGetter.getToMany(target);
                    if (toMany.getById(entityId) != null) {
                        toMany.removeById(entityId); // This is also done for non-persisted entities (if used elsewhere)
                        if (idGetter.getId(target) != 0) { // No further action for non-persisted entities required
                            if (removeFromTargetBox) {
                                entitiesToRemoveFromDb.add(target);
                            } else {
                                entitiesToPut.add(target);
                            }
                        }
                    }
                }
                setRemoved.clear();
            }
            return !entitiesToPut.isEmpty() || !entitiesToRemoveFromDb.isEmpty();
        }
    }

    /**
     * Like {@link #prepareToManyBacklinkEntitiesForDb} but for the linked ToOne relation.
     */
    private boolean prepareToOneBacklinkEntitiesForDb(long entityId, IdGetter<TARGET> idGetter,
                                                      @Nullable Map<TARGET, Boolean> setAdded, @Nullable Map<TARGET, Boolean> setRemoved) {
        ToOneGetter<TARGET, Object> backlinkToOneGetter = relationInfo.backlinkToOneGetter;

        synchronized (this) {
            if (setAdded != null && !setAdded.isEmpty()) {
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
            }

            if (setRemoved != null) {
                for (TARGET target : setRemoved.keySet()) {
                    ToOne<Object> toOne = backlinkToOneGetter.getToOne(target);
                    long toOneTargetId = toOne.getTargetId();
                    if (toOneTargetId == entityId) {
                        toOne.setTarget(null); // This is also done for non-persisted entities (if used elsewhere)
                        if (idGetter.getId(target) != 0) { // No further action for non-persisted entities required
                            if (removeFromTargetBox) {
                                entitiesToRemoveFromDb.add(target);
                            } else {
                                entitiesToPut.add(target);
                            }
                        }
                    }
                }
                setRemoved.clear();
            }
            return !entitiesToPut.isEmpty() || !entitiesToRemoveFromDb.isEmpty();
        }
    }

    /**
     * For internal use only; do not use in your app.
     * Convention: {@link #internalCheckApplyToDbRequired()} must be called before this call as it prepares .
     */
    @SuppressWarnings("unchecked") // Can't toArray(new TARGET[0]).
    @Internal
    public void internalApplyToDb(Cursor<?> sourceCursor, Cursor<TARGET> targetCursor) {
        TARGET[] toRemoveFromDb;
        TARGET[] toPut;
        TARGET[] addedStandalone = null;
        List<TARGET> removedStandalone = null;

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
                    entitiesToRemoveFromDb.addAll(entitiesRemoved.keySet());
                }
                if (!entitiesAdded.isEmpty()) {
                    addedStandalone = (TARGET[]) entitiesAdded.keySet().toArray();
                    entitiesAdded.clear();
                }
                if (!entitiesRemoved.isEmpty()) {
                    removedStandalone = new ArrayList<>(entitiesRemoved.keySet());
                    entitiesRemoved.clear();
                }
            }

            toRemoveFromDb = entitiesToRemoveFromDb.isEmpty() ? null : (TARGET[]) entitiesToRemoveFromDb.toArray();
            entitiesToRemoveFromDb.clear();
            toPut = entitiesToPut.isEmpty() ? null : (TARGET[]) entitiesToPut.toArray();
            entitiesToPut.clear();
        }

        if (toRemoveFromDb != null) {
            for (TARGET target : toRemoveFromDb) {
                long id = targetIdGetter.getId(target);
                if (id != 0) {
                    targetCursor.deleteEntity(id);
                }
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
                throw new IllegalStateException("Object with the ToMany has no ID (should have been put before)");
            }

            if (removedStandalone != null) {
                removeStandaloneRelations(sourceCursor, entityId, removedStandalone, targetIdGetter);
            }
            if (addedStandalone != null) {
                addStandaloneRelations(sourceCursor, entityId, addedStandalone, targetIdGetter);
            }
        }
    }

    /**
     * The list of removed entities may contain non-persisted entities, which will be ignored (removed from the list).
     */
    private void removeStandaloneRelations(Cursor<?> cursor, long sourceEntityId, List<TARGET> removed,
                                           IdGetter<TARGET> targetIdGetter) {
        Iterator<TARGET> iterator = removed.iterator();
        while (iterator.hasNext()) {
            if (targetIdGetter.getId(iterator.next()) == 0) {
                iterator.remove();
            }
        }

        int size = removed.size();
        if (size > 0) {
            long[] targetIds = new long[size];
            for (int i = 0; i < size; i++) {
                targetIds[i] = targetIdGetter.getId(removed.get(i));
            }
            cursor.modifyRelations(relationInfo.relationId, sourceEntityId, targetIds, true);
        }
    }

    /** The target array may not contain non-persisted entities. */
    private void addStandaloneRelations(Cursor<?> cursor, long sourceEntityId, TARGET[] added,
                                        IdGetter<TARGET> targetIdGetter) {
        int length = added.length;
        long[] targetIds = new long[length];
        for (int i = 0; i < length; i++) {
            long targetId = targetIdGetter.getId(added[i]);
            if (targetId == 0) {
                // Paranoia
                throw new IllegalStateException("Target object has no ID (should have been put before)");
            }
            targetIds[i] = targetId;
        }
        cursor.modifyRelations(relationInfo.relationId, sourceEntityId, targetIds, false);
    }

    /** For tests */
    Object getEntity() {
        return entity;
    }
}
