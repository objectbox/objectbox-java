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

import javax.annotation.concurrent.NotThreadSafe;

import io.objectbox.annotation.apihint.Experimental;
import io.objectbox.annotation.apihint.Internal;
import io.objectbox.internal.CursorFactory;

@Internal
@NotThreadSafe
@SuppressWarnings("WeakerAccess,UnusedReturnValue,unused")
public class Transaction implements Closeable {
    /** May be set by tests */
    @Internal
    static boolean TRACK_CREATION_STACK;

    private final long transaction;
    private final BoxStore store;
    private final boolean readOnly;
    private final Throwable creationThrowable;

    private int initialCommitCount;

    /** volatile because finalizer thread may interfere with "one thread, one TX" rule */
    private volatile boolean closed;

    static native void nativeDestroy(long transaction);

    static native int[] nativeCommit(long transaction);

    static native void nativeAbort(long transaction);

    static native void nativeReset(long transaction);

    static native void nativeRecycle(long transaction);

    static native void nativeRenew(long transaction);

    static native long nativeCreateKeyValueCursor(long transaction);

    static native long nativeCreateCursor(long transaction, String entityName, Class entityClass);

    //static native long nativeGetStore(long transaction);

    static native boolean nativeIsActive(long transaction);

    static native boolean nativeIsRecycled(long transaction);

    static native boolean nativeIsReadOnly(long transaction);

    public Transaction(BoxStore store, long transaction, int initialCommitCount) {
        this.store = store;
        this.transaction = transaction;
        this.initialCommitCount = initialCommitCount;
        readOnly = nativeIsReadOnly(transaction);

        creationThrowable = TRACK_CREATION_STACK ? new Throwable() : null;
    }

    @Override
    protected void finalize() throws Throwable {
        // Committed & aborted transactions are fine: remaining native resources are not expensive
        if (!closed && nativeIsActive(transaction)) { // TODO what about recycled state?
            System.err.println("Transaction was not finished (initial commit count: " + initialCommitCount + ").");
            if (creationThrowable != null) {
                System.err.println("Transaction was initially created here:");
                creationThrowable.printStackTrace();
            }
            System.err.flush();
        }
        close();
        super.finalize();
    }

    private void checkOpen() {
        if (closed) {
            throw new IllegalStateException("Transaction is closed");
        }
    }

    @Override
    public synchronized void close() {
        if (!closed) {
            closed = true;
            store.unregisterTransaction(this);

            // If store is already closed natively, destroying the tx would cause EXCEPTION_ACCESS_VIOLATION
            // TODO not destroying is probably only a small leak on rare occasions, but still could be fixed
            if (!store.isClosed()) {
                nativeDestroy(transaction);
            }
        }
    }

    public void commit() {
        checkOpen();
        int[] entityTypeIdsAffected = nativeCommit(transaction);
        store.txCommitted(this, entityTypeIdsAffected);
    }

    public void commitAndClose() {
        commit();
        close();
    }

    public void abort() {
        checkOpen();
        nativeAbort(transaction);
    }

    /**
     * Will throw if Cursors are still active for this TX.
     * Efficient for read transactions.
     */
    @Experimental
    public void reset() {
        checkOpen();
        initialCommitCount = store.commitCount;
        nativeReset(transaction);
    }

    /**
     * For read transactions, this releases important native resources that hold on versions of potential old data.
     * To continue, use {@link #renew()}.
     */
    public void recycle() {
        checkOpen();
        nativeRecycle(transaction);
    }

    /** Renews a previously recycled transaction (see {@link #recycle()}). Efficient for read transactions. */
    public void renew() {
        checkOpen();
        initialCommitCount = store.commitCount;
        nativeRenew(transaction);
    }

    public KeyValueCursor createKeyValueCursor() {
        checkOpen();
        long cursor = nativeCreateKeyValueCursor(transaction);
        return new KeyValueCursor(cursor);
    }

    public <T> Cursor<T> createCursor(Class<T> entityClass) {
        checkOpen();
        EntityInfo entityInfo = store.getEntityInfo(entityClass);
        CursorFactory<T> factory = entityInfo.getCursorFactory();
        long cursorHandle = nativeCreateCursor(transaction, entityInfo.getDbName(), entityClass);
        return factory.createCursor(this, cursorHandle, store);
    }

    public BoxStore getStore() {
        return store;
    }

    public boolean isActive() {
        checkOpen();
        return nativeIsActive(transaction);
    }

    public boolean isRecycled() {
        checkOpen();
        return nativeIsRecycled(transaction);
    }

    public boolean isClosed() {
        return closed;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    /**
     * Indicates if data returned from this transaction may be obsolete (another write TX was committed after this
     * transaction was started).
     */
    public boolean isObsolete() {
        return initialCommitCount != store.commitCount;
    }

    @Internal
    long internalHandle() {
        return transaction;
    }

    @Override
    public String toString() {
        return "TX " + Long.toString(transaction, 16) + " (" + (readOnly ? "read-only" : "write") +
                ", initialCommitCount=" + initialCommitCount + ")";
    }
}