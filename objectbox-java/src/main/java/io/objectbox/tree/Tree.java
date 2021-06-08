package io.objectbox.tree;

import io.objectbox.BoxStore;
import io.objectbox.InternalAccess;
import io.objectbox.Transaction;
import io.objectbox.annotation.apihint.Experimental;
import io.objectbox.model.PropertyType;

import javax.annotation.Nullable;
import java.util.concurrent.Callable;

/**
 * Points to a root branch, can traverse child branches and read and write data in leafs.
 */
@SuppressWarnings("SameParameterValue")
@Experimental
public class Tree {

    private long handle;
    private final BoxStore store;

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

    public BoxStore getStore() {
        return store;
    }

    public Branch root() {
        long dataBranchId = nativeGetRootId(handle);
        return new Branch(this, dataBranchId);
    }

    public void close() {
        long handle = this.handle;
        nativeDelete(handle);
        this.handle = 0;
    }

    public void runInTx(Runnable runnable) {
        store.runInTx(createTxRunnable(runnable));
    }

    public void runInReadTx(Runnable runnable) {
        store.runInReadTx(createTxRunnable(runnable));
    }

    public <T> T callInTx(Callable<T> callable) throws Exception {
        return store.callInTx(createTxCallable(callable));
    }

    /**
     * Wraps any Exception thrown by the callable into a RuntimeException.
     */
    public <T> T callInTxNoThrow(Callable<T> callable) {
        try {
            return store.callInTx(createTxCallable(callable));
        } catch (Exception e) {
            throw new RuntimeException("Callable threw exception", e);
        }
    }

    public <T> T callInReadTx(Callable<T> callable) {
        return store.callInReadTx(createTxCallable(callable));
    }

    private Runnable createTxRunnable(Runnable runnable) {
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
     * Get the leaf for the given ID or null if no leaf exists with that ID.
     */
    @Nullable
    public Leaf getLeaf(long id) {
        LeafNode leafNode = nativeGetLeafById(handle, id);
        if (leafNode == null) return null;
        return new Leaf(leafNode);
    }

    /**
     * Get data value for the given ID or null if no data leaf exists with that ID.
     */
    @Nullable
    public String getString(long id) {
        Leaf leaf = getLeaf(id);
        return leaf != null ? leaf.asString() : null;
    }

    /**
     * Get data value for the given ID or null if no data leaf exists with that ID.
     */
    @Nullable
    public Long getInteger(long id) {
        Leaf leaf = getLeaf(id);
        return leaf != null ? leaf.asInt() : null;
    }

    /**
     * Get data value for the given ID or null if no data leaf exists with that ID.
     */
    @Nullable
    public Double getDouble(long id) {
        Leaf leaf = getLeaf(id);
        return leaf != null ? leaf.asDouble() : null;
    }

    long putMetaBranch(long id, long parentBranchId, String name) {
        return nativePutMetaBranch(handle, id, parentBranchId, name, null);
    }

    long putMetaBranch(long id, long parentBranchId, String name, @Nullable String uid) {
        return nativePutMetaBranch(handle, id, parentBranchId, name, uid);
    }

    long[] putMetaBranches(String[] path) {
        return nativePutMetaBranches(handle, 0, path);
    }

    long[] putMetaBranches(long parentBranchId, String[] path) {
        return nativePutMetaBranches(handle, parentBranchId, path);
    }

    long putMetaLeaf(long id, long parentBranchId, String name, short valueType) {
        return nativePutMetaLeaf(handle, id, parentBranchId, name, valueType, false, null);
    }

    long putMetaLeaf(long id, long parentBranchId, String name, short valueType,
                     boolean isUnsigned) {
        return nativePutMetaLeaf(handle, id, parentBranchId, name, valueType, isUnsigned, null);
    }

    long putMetaLeaf(long id, long parentBranchId, String name, short valueType,
                     boolean isUnsigned, @Nullable String description) {
        return nativePutMetaLeaf(handle, id, parentBranchId, name, valueType, isUnsigned, description);
    }

    /**
     * Put a new or existing data branch
     */
    long putBranch(long id, long parentBranchId, long metaId, @Nullable String uid) {
        return nativePutBranch(handle, id, parentBranchId, metaId, uid);
    }

    /**
     * Put a new (inserts) data branch
     */
    long putBranch(long parentBranchId, long metaId, @Nullable String uid) {
        return nativePutBranch(handle, 0, parentBranchId, metaId, uid);
    }

    /**
     * Put a new (inserts) data branch
     */
    long putBranch(long parentBranchId, long metaId) {
        return nativePutBranch(handle, 0, parentBranchId, metaId, null);
    }

    long putValue(long id, long parentBranchId, long metaId, long value) {
        return nativePutValueInteger(handle, id, parentBranchId, metaId, value);
    }

    long putValue(long parentBranchId, long metaId, long value) {
        return nativePutValueInteger(handle, 0, parentBranchId, metaId, value);
    }

    long putValue(long parentBranchId, long metaId, double value) {
        return nativePutValueFP(handle, 0, parentBranchId, metaId, value);
    }

    long putValue(long id, long parentBranchId, long metaId, double value) {
        return nativePutValueFP(handle, id, parentBranchId, metaId, value);
    }

    long putValue(long id, long parentBranchId, long metaId, String value) {
        return nativePutValueString(handle, id, parentBranchId, metaId, value);
    }

    long putValue(long parentBranchId, long metaId, String value) {
        return nativePutValueString(handle, 0, parentBranchId, metaId, value);
    }

    public long put(Leaf leaf) {
        long id = leaf.getId();
        long parentId = leaf.getParentBranchId();
        long metaId = leaf.getMetaId();

        switch (leaf.getValueType()) {
            case PropertyType.Byte:
            case PropertyType.Char:
            case PropertyType.Short:
            case PropertyType.Int:
            case PropertyType.Long:
                return nativePutValueInteger(handle, id, parentId, metaId, leaf.getInt());
            case PropertyType.Float:
            case PropertyType.Double:
                return nativePutValueFP(handle, id, parentId, metaId, leaf.getDouble());
            case PropertyType.ByteVector:
            case PropertyType.String:
                // Note: using getString() as it also converts byte[]
                return nativePutValueString(handle, id, parentId, metaId, leaf.getString());
            default:
                throw new UnsupportedOperationException("Unsupported value type: " + leaf.getValueType());
        }
    }

    /**
     * Create a (Data)Tree instance for the given meta-branch root, or find a singular root if 0 is given.
     */
    static native long nativeCreate(long store, long rootId);

    /**
     * Not usable yet; TX is not aligned
     */
    static native long nativeCreateWithUid(long store, String uid);

    static native void nativeDelete(long handle);

    native boolean nativeSetTransaction(long handle, long txHandle);

    native void nativeClearTransaction(long handle);

    /**
     * Get the root data branch ID.
     */
    native long nativeGetRootId(long handle);

    native LeafNode nativeGetLeafById(long treeHandle, long leafId);

    native long nativePutMetaBranch(long treeHandle, long id, long parentBranchId, String name,
                                    @Nullable String description);

    native long[] nativePutMetaBranches(long treeHandle, long parentBranchId, String[] path);

    native long nativePutMetaLeaf(long treeHandle, long id, long parentBranchId, String name, short valueType,
                                  boolean isUnsigned, @Nullable String description);

    native long nativePutBranch(long treeHandle, long id, long parentBranchId, long metaId, @Nullable String uid);

    native long nativePutValueInteger(long treeHandle, long id, long parentBranchId, long metaId, long value);

    native long nativePutValueFP(long treeHandle, long id, long parentBranchId, long metaId, double value);

    native long nativePutValueString(long treeHandle, long id, long parentBranchId, long metaId, @Nullable String value);

}
