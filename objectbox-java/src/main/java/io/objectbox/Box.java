package io.objectbox;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import io.objectbox.annotation.apihint.Beta;
import io.objectbox.annotation.apihint.Internal;
import io.objectbox.exception.DbException;
import io.objectbox.internal.CallWithHandle;
import io.objectbox.internal.ReflectionCache;
import io.objectbox.query.QueryBuilder;

/**
 * A box to store objects of a particular class.
 * <p>
 * Thread-safe.
 */
@Beta
public class Box<T> {
    private final BoxStore store;
    private final Class<T> entityClass;

    /** Set when running inside TX */
    final ThreadLocal<Cursor<T>> activeTxCursor = new ThreadLocal<>();
    private final ThreadLocal<Cursor<T>> threadLocalReader = new ThreadLocal<>();
    private final List<WeakReference<Cursor<T>>> readers = new ArrayList<>();
    // TODO Add a new generated class for this (~"EntityOps", also with relation ID helpers?), using Cursor here is work-aroundish
    private final Cursor<T> idGetter;

    private Properties properties;
    private volatile Field boxStoreField;

    Box(BoxStore store, Class<T> entityClass) {
        this.store = store;
        this.entityClass = entityClass;
        Class<Cursor<T>> cursorClass = store.getEntityCursorClass(entityClass);
        try {
            idGetter = cursorClass.newInstance();
        } catch (Exception e) {
            throw new DbException("Box could not create cursor", e);
        }
    }

    private Cursor<T> getReader() {
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
                cursor.renew(tx);
            } else {
                cursor = store.beginReadTx().createCursor(entityClass);
                synchronized (readers) {
                    readers.add(new WeakReference<>(cursor));
                }
                threadLocalReader.set(cursor);
            }
        }
        return cursor;
    }

    private Cursor<T> getActiveTxCursor() {
        Transaction activeTx = store.activeTx.get();
        if (activeTx != null) {
            if (activeTx.isClosed()) {
                throw new IllegalStateException("Active TX is closed");
            }
            Cursor cursor = activeTxCursor.get();
            if (cursor == null || cursor.getTx().isClosed()) {
                cursor = activeTx.createCursor(entityClass);
                activeTxCursor.set(cursor);
            }
            return cursor;
        }
        return null;
    }

    private Cursor<T> getWriter() {
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

    private void commitWriter(Cursor<T> cursor) {
        // NOP if TX is ongoing
        if (activeTxCursor.get() == null) {
            cursor.close();
            cursor.getTx().commitAndClose();
        }
    }

    private void releaseWriter(Cursor<T> cursor) {
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

    private void releaseReader(Cursor<T> cursor) {
        // NOP if TX is ongoing
        if (activeTxCursor.get() == null) {
            Transaction tx = cursor.getTx();
            if (tx.isClosed() || tx.isRecycled() || !tx.isReadOnly()) {
                throw new IllegalStateException("Illegal reader TX state");
            }
            tx.recycle();
        }
    }

    void txCommitted(Transaction tx) {
        // TODO Unused readers should be disposed when a new write tx is committed
        // (readers hold on to old data pages and prevent to reuse them)

        // At least we should be able to clear the reader of the current thread if exists
        Cursor cursor = threadLocalReader.get();
        if (cursor != null) {
            threadLocalReader.remove();
            Transaction cursorTx = cursor.getTx();
            cursor.close();
            cursorTx.close();
        }

        cursor = activeTxCursor.get();
        if (cursor != null) {
            activeTxCursor.remove();
            cursor.close();
        }
    }

    public int getPropertyId(String propertyName) {
        Cursor<T> reader = getReader();
        try {
            return reader.getPropertyId(propertyName);
        } finally {
            releaseReader(reader);
        }
    }

    public long getId(T entity) {
        return idGetter.getId(entity);
    }

    public T get(long key) {
        Cursor<T> reader = getReader();
        try {
            return reader.get(key);
        } finally {
            releaseReader(reader);
        }

    }

    public long count() {
        Cursor<T> reader = getReader();
        try {
            return reader.count();
        } finally {
            releaseReader(reader);
        }
    }

    public List<T> find(String propertyName, String value) {
        Cursor<T> reader = getReader();
        try {
            return reader.find(propertyName, value);
        } finally {
            releaseReader(reader);
        }
    }

    public List<T> find(String propertyName, long value) {
        Cursor<T> reader = getReader();
        try {
            return reader.find(propertyName, value);
        } finally {
            releaseReader(reader);
        }
    }

    public List<T> find(int propertyId, long value) {
        Cursor<T> reader = getReader();
        try {
            return reader.find(propertyId, value);
        } finally {
            releaseReader(reader);
        }
    }

    public List<T> find(int propertyId, String value) {
        Cursor<T> reader = getReader();
        try {
            return reader.find(propertyId, value);
        } finally {
            releaseReader(reader);
        }
    }

    public List<T> find(Property property, String value) {
        Cursor<T> reader = getReader();
        try {
            return reader.find(property.dbName, value);
        } finally {
            releaseReader(reader);
        }
    }

    public List<T> find(Property property, long value) {
        Cursor<T> reader = getReader();
        try {
            return reader.find(property.dbName, value);
        } finally {
            releaseReader(reader);
        }
    }

    public List<T> getAll() {
        Cursor<T> cursor = getReader();
        try {
            T first = cursor.first();
            if (first == null) {
                return Collections.emptyList();
            } else {
                ArrayList<T> list = new ArrayList<>();
                list.add(first);
                while (true) {
                    T next = cursor.next();
                    if (next != null) {
                        list.add(next);
                    } else {
                        break;
                    }
                }
                return list;
            }
        } finally {
            releaseReader(cursor);
        }
    }

    /** Does not work yet, also probably won't be faster than {@link Box#getAll()}. */
    public List<T> getAll2() {
        Cursor<T> reader = getReader();
        try {
            return reader.getAll();
        } finally {
            releaseReader(reader);
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
    public void put(T... entities) {
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
     */
    public void put(Collection<T> entities) {
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

    public void remove(long key) {
        Cursor<T> cursor = getWriter();
        try {
            cursor.deleteEntity(key);
            commitWriter(cursor);
        } finally {
            releaseWriter(cursor);
        }
    }

    public void remove(long... keys) {
        if (keys == null || keys.length == 0) {
            return;
        }

        Cursor<T> cursor = getWriter();
        try {
            for (long key : keys) {
                cursor.deleteEntity(key);
            }
            commitWriter(cursor);
        } finally {
            releaseWriter(cursor);
        }
    }

    /**
     * Due to type erasure collision, we cannot simply use "remove" as a method name here.
     */
    public void removeByKeys(Collection<Long> keys) {
        if (keys == null || keys.isEmpty()) {
            return;
        }
        Cursor<T> cursor = getWriter();
        try {
            for (long key : keys) {
                cursor.deleteEntity(key);
            }
            commitWriter(cursor);
        } finally {
            releaseWriter(cursor);
        }
    }

    public void remove(T entity) {
        Cursor<T> cursor = getWriter();
        try {
            long key = cursor.getId(entity);
            cursor.deleteEntity(key);
            commitWriter(cursor);
        } finally {
            releaseWriter(cursor);
        }
    }

    public void remove(T... entities) {
        if (entities == null || entities.length == 0) {
            return;
        }
        Cursor<T> cursor = getWriter();
        try {
            for (T entity : entities) {
                long key = cursor.getId(entity);
                cursor.deleteEntity(key);
            }
            commitWriter(cursor);
        } finally {
            releaseWriter(cursor);
        }
    }

    public void remove(Collection<T> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }
        Cursor<T> cursor = getWriter();
        try {
            for (T entity : entities) {
                long key = cursor.getId(entity);
                cursor.deleteEntity(key);
            }
            commitWriter(cursor);
        } finally {
            releaseWriter(cursor);
        }
    }

    public void removeAll() {
        Cursor<T> cursor = getWriter();
        try {
            cursor.deleteAll();
            commitWriter(cursor);
        } finally {
            releaseWriter(cursor);
        }
    }

    public QueryBuilder<T> query() {
        return new QueryBuilder<T>(this, store.internalHandle(), store.getEntityName(entityClass));
    }

    public BoxStore getStore() {
        return store;
    }

    // Returned Property object will have an ID set
    public synchronized Properties getProperties() {
        if (properties == null) {
            Cursor<T> reader = getReader();
            try {
                properties = reader.getProperties();
            } finally {
                releaseReader(reader);
            }
        }
        return properties;
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

    public Class<T> getEntityClass() {
        return entityClass;
    }

    @Internal
    public List<T> getBacklinkEntities(int entityId, Property relationIdProperty, long key) {
        Cursor<T> reader = getReader();
        try {
            return reader.getBacklinkEntities(entityId, relationIdProperty, key);
        } finally {
            releaseReader(reader);
        }
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

}