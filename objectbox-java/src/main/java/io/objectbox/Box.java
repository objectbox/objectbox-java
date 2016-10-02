package io.objectbox;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * A box to store objects of a particular class.
 * <p>
 * Thread-safe.
 */
public class Box<T> {
    private final BoxStore store;
    private final Class<T> entityClass;

    /** Set when running inside TX */
    final ThreadLocal<Cursor<T>> txCursor = new ThreadLocal<>();
    private final ThreadLocal<Cursor<T>> threadLocalReader = new ThreadLocal<>();
    private final List<WeakReference<Cursor<T>>> readers = new ArrayList<>();

    Box(BoxStore store, Class<T> entityClass) {
        this.store = store;
        this.entityClass = entityClass;
    }

    private Cursor<T> getReader() {
        Cursor<T> cursor = getTxCursor();
        if (cursor != null) {
            return cursor;
        } else {
            cursor = threadLocalReader.get();
            if (cursor == null || cursor.isObsolete()) {
                if (cursor != null) {
                    cursor.close();
                }
                cursor = store.sharedReadTx().createCursor(entityClass);
                synchronized (readers) {
                    readers.add(new WeakReference<>(cursor));
                }
                threadLocalReader.set(cursor);
            }
        }
        return cursor;
    }

    private Cursor<T> getTxCursor() {
        Transaction activeTx = store.activeTx.get();
        if (activeTx != null) {
            if (activeTx.isClosed()) {
                throw new IllegalStateException("Active TX is closed");
            }
            Cursor cursor = txCursor.get();
            if (cursor == null || cursor.getTx().isClosed()) {
                cursor = activeTx.createCursor(entityClass);
                txCursor.set(cursor);
            }
            return cursor;
        }
        return null;
    }

    private Cursor<T> getWriter() {
        Cursor cursor = getTxCursor();
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
        if (txCursor.get() == null) {
            cursor.close();
            cursor.getTx().commitAndClose();
        }
    }

    private void releaseWriter(Cursor<T> cursor) {
        // NOP if TX is ongoing
        if (txCursor.get() == null) {
            Transaction tx = cursor.getTx();
            if (!tx.isClosed()) {
                cursor.close();
                tx.abort();
                tx.close();
            }
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

        cursor = txCursor.get();
        if (cursor != null) {
            txCursor.remove();
            cursor.close();
        }
    }

    public int getPropertyId(String propertyName) {
        return getReader().getPropertyId(propertyName);
    }

    public T get(long key) {
        return getReader().get(key);
    }

    public long count() {
        return getReader().count();
    }

    public List<T> find(String propertyName, String value) {
        return getReader().find(propertyName, value);
    }

    public List<T> find(String propertyName, long value) {
        return getReader().find(propertyName, value);
    }

    public List<T> find(int propertyId, long value) {
        return getReader().find(propertyId, value);
    }

    public List<T> find(int propertyId, String value) {
        return getReader().find(propertyId, value);
    }

    public List<T> find(Property property, String value) {
        return getReader().find(property.dbName, value);
    }

    public List<T> find(Property property, long value) {
        return getReader().find(property.dbName, value);
    }

    public List<T> getAll() {
        Cursor<T> cursor = getReader();
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
    }

    /** Does not work yet, also probably won't be faster than {@link Box#getAll()}. */
    public List<T> getAll2() {
        return getReader().getAll();
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

    public BoxStore getStore() {
        return store;
    }
}