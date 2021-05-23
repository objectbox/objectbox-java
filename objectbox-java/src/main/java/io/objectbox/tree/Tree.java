package io.objectbox.tree;

import io.objectbox.BoxStore;

/**
 * Points to a root branch, can traverse child branches and read and write data in leafs.
 */
public class Tree {

    private long handle;

    /**
     * Create a tree instance for the given meta-branch root {@code uid}, or find a singular root if 0 is given.
     */
    public Tree(BoxStore store, String uid) {
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

    /**
     * Create a (Data)Tree instance for the given meta-branch root, or find a singular root if 0 is given.
     */
    private static native long nativeCreate(long store, long rootId);

    /** Not usable yet; TX is not aligned */
    private static native long nativeCreateWithUid(long store, String uid);

    private static native void nativeDelete(long handle);

    /**
     * Get the root data branch ID.
     */
    private native long nativeRoot(long handle);

}
