package io.objectbox;

import java.io.Closeable;

import javax.annotation.concurrent.NotThreadSafe;

import io.objectbox.annotation.apihint.Internal;
import io.objectbox.internal.CursorFactory;

@Internal
@NotThreadSafe
public class Transaction implements Closeable {
    static final boolean WARN_FINALIZER = false;

    private final long transaction;
    private final BoxStore store;
    private final boolean readOnly;
    private final Throwable creationThrowable;

    private int initialCommitCount;
    private boolean closed;

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

        creationThrowable = WARN_FINALIZER ? new Throwable() : null;
    }

    @Override
    protected void finalize() throws Throwable {
        if (WARN_FINALIZER && !closed && creationThrowable != null) {
            System.err.println("Transaction was not closed. It was initially created here:");
            creationThrowable.printStackTrace();
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

    /** Efficient for read transactions. */
    public void reset() {
        checkOpen();
        initialCommitCount = store.commitCount;
        nativeReset(transaction);
    }

    /** For read transactions. */
    public void recycle() {
        checkOpen();
        nativeRecycle(transaction);
    }

    /** Efficient for read transactions. */
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