package io.objectbox.tree;

import io.objectbox.BoxStore;
import io.objectbox.InternalAccess;
import io.objectbox.Transaction;

import java.util.concurrent.Callable;

/**
 * Points to a root branch, can traverse child branches and read and write data in leafs.
 */
public class Tree {

    private long handle;
    private BoxStore store;

    /**
     * Create a tree instance for the given meta-branch root {@code uid}, or find a singular root if 0 is given.
     */
    public Tree(BoxStore store, String uid) {
        this.store = store;
        //noinspection ConstantConditions Nullability annotations are not enforced.
        if (store == null) {
            throw new IllegalArgumentException("store must not be null");
        }
        //noinspection ConstantConditions Nullability annotations are not enforced.
        if (uid == null || uid.length() == 0) {
            throw new IllegalArgumentException("uid must be 0 or not empty");
        }
        this.handle = nativeCreateWithUid(store.getNativeStore(), uid);
    }

    /**
     * Create a tree instance for the given meta-branch root {@code uid}, or find a singular root if 0 is given.
     */
    public Tree(BoxStore store, long rootId) {
        this.store = store;
        //noinspection ConstantConditions Nullability annotations are not enforced.
        if (store == null) {
            throw new IllegalArgumentException("store must not be null");
        }
        this.handle = nativeCreate(store.getNativeStore(), rootId);
    }

    long getHandle() {
        return handle;
    }

    public Branch root() {
        long dataBranchId = nativeRoot(handle);
        return new Branch(this, dataBranchId);
    }

    public void close() {
        long handle = this.handle;
        nativeDelete(handle);
        this.handle = 0;
    }

    public void runInTx(Runnable runnable) {
        store.runInTx(createTxCallable(runnable));
    }

    public void runInReadTx(Runnable runnable) {
        store.runInReadTx(createTxCallable(runnable));
    }

    public <T> T callInTx(Callable<T> callable) throws Exception {
        return store.callInTx(createTxCallable(callable));
    }

    public <T> T callInReadTx(Callable<T> callable) throws Exception {
        return store.callInReadTx(createTxCallable(callable));
    }

    private Runnable createTxCallable(Runnable runnable) {
        return () -> {
            Transaction tx = InternalAccess.getActiveTx(store);
            boolean topLevel = nativeSetTransaction(handle, InternalAccess.getHandle(tx));
            try {
                runnable.run();
            } finally {
                if (topLevel) nativeClearTransaction(handle);
            }
        };
    }

    private <T> Callable<T> createTxCallable(Callable<T> callable) {
        return () -> {
            Transaction tx = InternalAccess.getActiveTx(store);
            boolean topLevel = nativeSetTransaction(handle, InternalAccess.getHandle(tx));
            try {
                return callable.call();
            } finally {
                if (topLevel) nativeClearTransaction(handle);
            }
        };
    }

    /**
     * Create a (Data)Tree instance for the given meta-branch root, or find a singular root if 0 is given.
     */
    private static native long nativeCreate(long store, long rootId);

    /** Not usable yet; TX is not aligned */
    private static native long nativeCreateWithUid(long store, String uid);

    private static native void nativeDelete(long handle);

    private native boolean nativeSetTransaction(long handle, long txHandle);

    private native void nativeClearTransaction(long handle);

    /**
     * Get the root data branch ID.
     */
    private native long nativeRoot(long handle);

}
