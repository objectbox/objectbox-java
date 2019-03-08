/*
 * Copyright 2017 ObjectBox Ltd. All rights reserved.
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

package io.objectbox;

import java.io.Closeable;
import java.util.List;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import io.objectbox.annotation.apihint.Beta;
import io.objectbox.annotation.apihint.Internal;
import io.objectbox.annotation.apihint.Temporary;
import io.objectbox.relation.ToMany;

@SuppressWarnings({"unchecked", "SameParameterValue", "unused", "WeakerAccess", "UnusedReturnValue"})
@Beta
@Internal
@NotThreadSafe
public abstract class Cursor<T> implements Closeable {
    /** May be set by tests */
    @Internal
    static boolean TRACK_CREATION_STACK;

    @Internal
    static boolean LOG_READ_NOT_CLOSED;

    protected static final int PUT_FLAG_FIRST = 1;
    protected static final int PUT_FLAG_COMPLETE = 1 << 1;

    static native void nativeDestroy(long cursor);

    static native void nativeDeleteEntity(long cursor, long key);

    static native void nativeDeleteAll(long cursor);

    static native boolean nativeSeek(long cursor, long key);

    static native Object nativeGetAllEntities(long cursor);

    static native Object nativeGetEntity(long cursor, long key);

    static native Object nativeNextEntity(long cursor);

    static native Object nativeFirstEntity(long cursor);

    static native long nativeCount(long cursor, long maxCountOrZero);

    // TODO not implemented
    static native long nativeGetKey(long cursor);

    static native long nativeLookupKeyUsingIndex(long cursor, int propertyId, String value);

    static native long nativeRenew(long cursor);

    protected static native long collect313311(long cursor, long keyIfComplete, int flags,
                                               int idStr1, @Nullable String valueStr1,
                                               int idStr2, @Nullable String valueStr2,
                                               int idStr3, @Nullable String valueStr3,
                                               int idBA1, @Nullable byte[] valueBA1,
                                               int idLong1, long valueLong1, int idLong2, long valueLong2,
                                               int idLong3, long valueLong3,
                                               int idInt1, int valueInt1, int idInt2, int valueInt2,
                                               int idInt3, int valueInt3,
                                               int idFloat1, float valueFloat1, int idDouble1, double valueDouble1
    );

    protected static native long collect430000(long cursor, long keyIfComplete, int flags,
                                               int idStr1, @Nullable String valueStr1,
                                               int idStr2, @Nullable String valueStr2,
                                               int idStr3, @Nullable String valueStr3,
                                               int idStr4, @Nullable String valueStr4,
                                               int idBA1, @Nullable byte[] valueBA1,
                                               int idBA2, @Nullable byte[] valueBA2,
                                               int idBA3, @Nullable byte[] valueBA3
    );

    protected static native long collect400000(long cursor, long keyIfComplete, int flags,
                                               int idStr1, @Nullable String valueStr1,
                                               int idStr2, @Nullable String valueStr2,
                                               int idStr3, @Nullable String valueStr3,
                                               int idStr4, @Nullable String valueStr4
    );

    protected static native long collect002033(long cursor, long keyIfComplete, int flags,
                                               int idLong1, long valueLong1, int idLong2, long valueLong2,
                                               int idFloat1, float valueFloat1, int idFloat2, float valueFloat2,
                                               int idFloat3, float valueFloat3,
                                               int idDouble1, double valueDouble1, int idDouble2, double valueDouble2,
                                               int idDouble3, double valueDouble3
    );

    protected static native long collect004000(long cursor, long keyIfComplete, int flags,
                                               int idLong1, long valueLong1, int idLong2, long valueLong2,
                                               int idLong3, long valueLong3, int idLong4, long valueLong4
    );

    static native int nativePropertyId(long cursor, String propertyValue);

    static native List nativeGetBacklinkEntities(long cursor, int entityId, int propertyId, long key);

    static native List nativeGetRelationEntities(long cursor, int sourceEntityId, int relationId, long key, boolean backlink);

    static native void nativeModifyRelations(long cursor, int relationId, long key, long[] targetKeys, boolean remove);

    static native void nativeModifyRelationsSingle(long cursor, int relationId, long key, long targetKey, boolean remove);

    static native void nativeSetBoxStoreForEntities(long cursor, Object boxStore);

    protected final Transaction tx;
    protected final long cursor;
    protected final EntityInfo entityInfo;
    protected final BoxStore boxStoreForEntities;

    protected final boolean readOnly;
    protected boolean closed;

    private final Throwable creationThrowable;

    protected Cursor(Transaction tx, long cursor, EntityInfo entityInfo, BoxStore boxStore) {
        if (tx == null) {
            throw new IllegalArgumentException("Transaction is null");
        }
        this.tx = tx;
        readOnly = tx.isReadOnly();
        this.cursor = cursor;
        this.entityInfo = entityInfo;
        this.boxStoreForEntities = boxStore;

        Property[] allProperties = entityInfo.getAllProperties();
        for (Property property : allProperties) {
            if (!property.isIdVerified()) {
                int id = getPropertyId(property.dbName);
                property.verifyId(id);
            }
        }
        creationThrowable = TRACK_CREATION_STACK ? new Throwable() : null;

        nativeSetBoxStoreForEntities(cursor, boxStore);
    }

    @Override
    protected void finalize() throws Throwable {
        if (!closed) {
            // By default only complain about write cursors
            if (!readOnly || LOG_READ_NOT_CLOSED) {
                System.err.println("Cursor was not closed.");
                if (creationThrowable != null) {
                    System.err.println("Cursor was initially created here:");
                    creationThrowable.printStackTrace();
                }
                System.err.flush();
            }
            close();
            super.finalize();
        }
    }

    protected abstract long getId(T entity);

    public abstract long put(T entity);

    public EntityInfo getEntityInfo() {
        return entityInfo;
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

    /** ~10% slower than iterating with {@link #first()} and {@link #next()} as done by {@link Box#getAll()}. */
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

    public long count(long maxCountOrZero) {
        return nativeCount(cursor, maxCountOrZero);
    }

    @Override
    public synchronized void close() {
        if (!closed) {
            closed = true;
            // tx is null despite check in constructor in some tests (called by finalizer):
            // Null check avoids NPE in finalizer and seems to stabilize Android instrumentation perf tests.
            if (tx != null && !tx.getStore().isClosed()) {
                nativeDestroy(cursor);
            }
        }
    }

    public int getPropertyId(String propertyName) {
        return nativePropertyId(cursor, propertyName);
    }

    /**
     * @return key or 0 if not found
     * @deprecated TODO only used in tests, remove in the future
     */
    long lookupKeyUsingIndex(int propertyId, String value) {
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

    protected <TARGET> Cursor<TARGET> getRelationTargetCursor(Class<TARGET> targetClass) {
        // minor to do: optimize by using existing native cursor handle?
        // (Note: Cursor should not destroy the native cursor then.)

        return tx.createCursor(targetClass);
    }

    /**
     * To be used in combination with {@link Transaction#renew()}.
     */
    public void renew() {
        nativeRenew(cursor);
    }

    @Internal
    long internalHandle() {
        return cursor;
    }

    @Internal
    List<T> getBacklinkEntities(int entityId, Property relationIdProperty, long key) {
        try {
            return nativeGetBacklinkEntities(cursor, entityId, relationIdProperty.getId(), key);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Please check if the given property belongs to a valid @Relation: "
                    + relationIdProperty, e);
        }
    }

    @Internal
    public List<T> getRelationEntities(int sourceEntityId, int relationId, long key, boolean backlink) {
        return nativeGetRelationEntities(cursor, sourceEntityId, relationId, key, backlink);
    }

    @Internal
    public void modifyRelations(int relationId, long key, long[] targetKeys, boolean remove) {
        nativeModifyRelations(cursor, relationId, key, targetKeys, remove);
    }

    @Internal
    public void modifyRelationsSingle(int relationId, long key, long targetKey, boolean remove) {
        nativeModifyRelationsSingle(cursor, relationId, key, targetKey, remove);
    }

    protected <TARGET> void checkApplyToManyToDb(List<TARGET> orders, Class<TARGET> targetClass) {
        if (orders instanceof ToMany) {
            ToMany<TARGET> toMany = (ToMany<TARGET>) orders;
            if (toMany.internalCheckApplyToDbRequired()) {
                Cursor<TARGET> targetCursor = getRelationTargetCursor(targetClass);
                try {
                    toMany.internalApplyToDb(this, targetCursor);
                } finally {
                    targetCursor.close();
                }
            }
        }
    }

    @Override
    public String toString() {
        return "Cursor " + Long.toString(cursor, 16) + (isClosed() ? "(closed)" : "");
    }
}
