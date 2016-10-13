package io.objectbox;

import java.io.Closeable;
import java.util.List;

import io.objectbox.annotation.apihint.Beta;
import io.objectbox.annotation.apihint.Internal;

/**
 * Created by markus.
 */
@Beta
public abstract class Cursor<T> implements Closeable {
    protected static final int PUT_FLAG_FIRST = 1 << 0;
    protected static final int PUT_FLAG_COMPLETE = 1 << 1;

    static native void nativeDestroy(long cursor);

    static native void nativeDeleteEntity(long cursor, long key);

    static native void nativeDeleteAll(long cursor);

    static native boolean nativeSeek(long cursor, long key);

    static native Object nativeGetAllEntities(long cursor);

    static native Object nativeGetEntity(long cursor, long key);

    static native Object nativeNextEntity(long cursor);

    static native Object nativeFirstEntity(long cursor);

    static native long nativeCount(long cursor);

    static native List nativeFindScalar(long cursor, String propertyName, long value);

    static native List nativeFindString(long cursor, String propertyName, String value);

    static native List nativeFindScalarPropertyId(long cursor, int propertyId, long value);

    static native List nativeFindStringPropertyId(long cursor, int propertyId, String value);

    // TODO not implemented
    static native long nativeGetKey(long cursor);

    static native long nativeLookupKeyUsingIndex(long cursor, int propertyId, String value);

    protected static native long collect313311(long cursor, long keyIfComplete, int flags,
                                               int idStr1, String valueStr1, int idStr2, String valueStr2,
                                               int idStr3, String valueStr3,
                                               int idBA1, byte[] valueBA1,
                                               int idLong1, long valueLong1, int idLong2, long valueLong2,
                                               int idLong3, long valueLong3,
                                               int idInt1, int valueInt1, int idInt2, int valueInt2,
                                               int idInt3, int valueInt3,
                                               int idFloat1, float valueFloat1, int idDouble1, double valueDouble1
    );

    static native void nativePut4000(long cursor,
                                     int id1, String value1, int id2, String value2,
                                     int id3, String value3, int id4, String value4,
                                     boolean complete
    );

    static native int nativePropertyId(long cursor, String propertyValue);

    protected final Transaction tx;
    protected final long cursor;
    protected final Properties properties;

    protected boolean closed;

    protected Cursor(Transaction tx, long cursor, Properties properties) {
        this.tx = tx;
        this.cursor = cursor;
        this.properties = properties;
    }

    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

    protected abstract long getId(T entity);

    public abstract long put(T entity);

    public Properties getProperties() {
        return properties;
    }

    public T get(long key) {
        return (T) nativeGetEntity(cursor, key);
    }

    public T next() {
        return (T) nativeNextEntity(cursor);
    }

    public T first() {
        return (T) nativeFirstEntity(cursor);
    }

    /** Does not work yet, also probably won't be faster than {@link Box#getAll()}. */
    public List<T> getAll() {
        return (List) nativeGetAllEntities(cursor);
    }

    public void deleteEntity(long key) {
        nativeDeleteEntity(cursor, key);
    }

    public void deleteAll() {
        nativeDeleteAll(cursor);
    }

    public long getKey() {
        return nativeGetKey(cursor);
    }

    public boolean seek(long key) {
        return nativeSeek(cursor, key);
    }

    public long count() {
        return nativeCount(cursor);
    }

    @Override
    public synchronized void close() {
        if (!closed) {
            closed = true;
            // TODO Improve native destroy?
            if (!tx.isClosed() && !tx.getStore().isClosed()) {
                nativeDestroy(cursor);
            }
        }
    }

    public int getPropertyId(String propertyName) {
        return nativePropertyId(cursor, propertyName);
    }

    public List<T> find(String propertyName, long value) {
        return nativeFindScalar(cursor, propertyName, value);
    }

    public List<T> find(String propertyName, String value) {
        return nativeFindString(cursor, propertyName, value);
    }

    public List<T> find(int propertyId, long value) {
        return nativeFindScalarPropertyId(cursor, propertyId, value);
    }

    public List<T> find(int propertyId, String value) {
        return nativeFindStringPropertyId(cursor, propertyId, value);
    }

    /**
     * @return key or 0 if not found
     */
    public long lookupKeyUsingIndex(int propertyId, String value) {
        return nativeLookupKeyUsingIndex(cursor, propertyId, value);
    }

    public Transaction getTx() {
        return tx;
    }

    // This cursor may operate on obsolete data (another write TX was committed after this cursor's TX had begun).
    public boolean isObsolete() {
        return tx.isObsolete();
    }

    public boolean isClosed() {
        return closed;
    }

    @Internal
    long internalHandle() {
        return cursor;
    }
}
