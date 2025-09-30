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

package io.objectbox;

import java.io.Closeable;

import javax.annotation.concurrent.NotThreadSafe;

import io.objectbox.annotation.apihint.Experimental;
import io.objectbox.annotation.apihint.Internal;
import io.objectbox.exception.DbException;
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

    native void nativeDestroy(long transaction);

    native int[] nativeCommit(long transaction);

    native void nativeAbort(long transaction);

    native void nativeReset(long transaction);

    native void nativeRecycle(long transaction);

    native void nativeRenew(long transaction);

    native long nativeCreateKeyValueCursor(long transaction);

    native long nativeCreateCursor(long transaction, String entityName, Class<?> entityClass);

    // native long nativeGetStore(long transaction);

    native boolean nativeIsActive(long transaction);

    native boolean nativeIsOwnerThread(long transaction);

    native boolean nativeIsRecycled(long transaction);

    native boolean nativeIsReadOnly(long transaction);

    public Transaction(BoxStore store, long transaction, int initialCommitCount) {
        this.store = store;
        this.transaction = transaction;
        this.initialCommitCount = initialCommitCount;
        readOnly = nativeIsReadOnly(transaction);

        creationThrowable = TRACK_CREATION_STACK ? new Throwable() : null;
    }

    /**
     * Explicitly call {@link #close()} instead to avoid expensive finalization.
     */
    @SuppressWarnings("deprecation") // finalize()
    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

    void checkOpen() {
        if (closed) {
            throw new IllegalStateException("Transaction is closed");
        }
    }

    @Override
    public synchronized void close() {
        if (!closed) {
            // Closeable recommendation: mark as closed before any code that might throw.
            closed = true;
            store.unregisterTransaction(this);

            boolean isOwnerThread = nativeIsOwnerThread(transaction);
            if (!isOwnerThread) {
                // Note: don't use isActive(), it returns false here because closed == true already
                boolean isActive = nativeIsActive(transaction);
                boolean isRecycled = nativeIsRecycled(transaction);
                if (isActive || isRecycled) {
                    String msgPostfix = " (initial commit count: " + initialCommitCount + ").";
                    if (isActive) {
                        System.err.println("Transaction is still active" + msgPostfix);
                    } else {
                        // This is not uncommon when using Box; as it keeps a thread-local Cursor and recycles the TX
                        System.out.println("Hint: use closeThreadResources() to avoid finalizing recycled transactions"
                                + msgPostfix);
                        System.out.flush();
                    }
                    if (creationThrowable != null) {
                        System.err.println("Transaction was initially created here:");
                        creationThrowable.printStackTrace();
                    }
                    System.err.flush();
                }
            }

            // If store is already closed natively, destroying the tx would cause EXCEPTION_ACCESS_VIOLATION
            // TODO not destroying is probably only a small leak on rare occasions, but still could be fixed
            if (!store.isNativeStoreClosed()) {
                nativeDestroy(transaction);
            } else {
                // Note: don't use isActive(), it returns false here because closed == true already
                boolean isActive = nativeIsActive(transaction);
                if (readOnly) {
                    // Minor leak if TX is active, but still log so the ObjectBox team can check that it only happens
                    // occasionally.
                    // Note this cannot assume the store isn't destroyed, yet. The native and Java stores may at best
                    // briefly wait for read transactions.
                    System.out.printf(
                            "Info: closing read transaction after store was closed (isActive=%s, isOwnerThread=%s), this should be avoided.%n",
                            isActive, isOwnerThread);
                    System.out.flush();

                    // Note: get fresh active state
                    if (!nativeIsActive(transaction)) {
                        nativeDestroy(transaction);
                    }
                } else {
                    // write transaction
                    System.out.printf(
                            "WARN: closing write transaction after store was closed (isActive=%s, isOwnerThread=%s), this must be avoided.%n",
                            isActive, isOwnerThread);
                    System.out.flush();

                    // Note: get fresh active state
                    if (nativeIsActive(transaction) && store.isNativeStoreDestroyed()) {
                        // This is an internal validation: if this is an active write-TX,
                        // the (native) store will always wait for it, so it must not be destroyed yet.
                        // If this ever happens, the above assumption is wrong, and throwing likely prevents a SIGSEGV.
                        throw new IllegalStateException(
                                "Internal error: cannot close active write transaction for an already destroyed store");
                    }
                    // Note: inactive transactions are always safe to destroy, regardless of store state and thread.
                    // Note: the current native impl panics if the transaction is active AND created in another thread.
                    nativeDestroy(transaction);
                }
            }
        }
    }

    /**
     * For a write transaction commits the changes. For a read transaction throws.
     */
    public void commit() {
        checkOpen();
        int[] entityTypeIdsAffected = nativeCommit(transaction);
        store.txCommitted(this, entityTypeIdsAffected);
    }

    public void commitAndClose() {
        commit();
        close();
    }

    /**
     * For a read or write transaction, aborts it.
     */
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
        EntityInfo<T> entityInfo = store.getEntityInfo(entityClass);
        CursorFactory<T> factory = entityInfo.getCursorFactory();
        long cursorHandle = nativeCreateCursor(transaction, entityInfo.getDbName(), entityClass);
        if (cursorHandle == 0) throw new DbException("Could not create native cursor");
        return factory.createCursor(this, cursorHandle, store);
    }

    public BoxStore getStore() {
        return store;
    }

    /**
     * A transaction is active after it was created until {@link #close()}, {@link #abort()}, or, for write
     * transactions only, {@link #commit()} is called.
     *
     * @return If this transaction is active.
     */
    public boolean isActive() {
        return !closed && nativeIsActive(transaction);
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