/*
 * Copyright 2021 ObjectBox Ltd.
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

package io.objectbox.tree;

import java.io.Closeable;
import java.util.concurrent.Callable;

import javax.annotation.Nullable;

import io.objectbox.BoxStore;
import io.objectbox.InternalAccess;
import io.objectbox.Transaction;
import io.objectbox.annotation.apihint.Experimental;
import io.objectbox.model.PropertyType;

/**
 * A higher level tree API operating on branch and leaf nodes.
 * Points to a root branch, can traverse child branches and read and write data in leafs.
 * <p>
 * Depends on a compatible tree model (entity types), which is matched by type and property names.
 * E.g. names like "DataLeaf.valueString" are fixed and may not be changed.
 * Adding properties to tree types is allowed.
 * <p>
 * Note there are TWO ways to work with tree data (both ways can be mixed):
 * - Standard ObjectBox entity types with e.g. Box&lt;DataLeaf&gt;
 * - Higher level tree API via this Tree class
 * <p>
 * To navigate in the tree, you typically start with {@link #getRoot()}, which returns a {@link Branch}.
 * From one branch you can navigate to other branches and also {@link Leaf}s, which carry data attributes.
 * <p>
 * You can easily navigate the tree by using a path, which is either a string or a string array.
 * A path refers to the names of the meta tree nodes, e.g. "Book.Author.Name".
 * <p>
 * If you already know the IDs, you can efficiently access branches, data leaves, and data values directly.
 * <p>
 * To access any data in the tree, you must use explicit transactions offer by methods such as
 * {@link #runInTx(Runnable)}, {@link #runInReadTx(Runnable)}, {@link #callInTx(Callable)}, or
 * {@link #callInReadTx(Callable)}.
 */
@SuppressWarnings("SameParameterValue")
@Experimental
public class Tree implements Closeable {

    private long handle;
    private final BoxStore store;
    private long rootId;
    private String pathSeparatorRegex = "\\.";

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
     * Create a tree instance for the given data-branch root ID.
     */
    public Tree(BoxStore store, long rootId) {
        this.store = store;
        this.rootId = rootId;
        //noinspection ConstantConditions Nullability annotations are not enforced.
        if (store == null) {
            throw new IllegalArgumentException("store must not be null");
        }
        this.handle = nativeCreate(store.getNativeStore(), rootId);
    }

    long getHandle() {
        return handle;
    }

    /**
     * The path separator regex is used to split a string path into individual path names.
     * Example: with the default separator, e.g. "Book.Author" becomes ["Book", "Author"].
     */
    public String getPathSeparatorRegex() {
        return pathSeparatorRegex;
    }

    /** E.g. use "\\/" to change path strings to "Book/Author"; see {@link #getPathSeparatorRegex()} for details. */
    public void setPathSeparatorRegex(String pathSeparatorRegex) {
        this.pathSeparatorRegex = pathSeparatorRegex;
    }

    /**
     * The root ID, which the tree was constructed with.
     */
    public long getRootId() {
        return rootId;
    }

    public BoxStore getStore() {
        return store;
    }

    /**
     * Gets the root of the data tree.
     */
    public Branch getRoot() {
        long dataBranchId = nativeGetRootId(handle);
        return new Branch(this, dataBranchId);
    }

    /**
     * Cleans up any (native) resources associated with this tree.
     */
    public void close() {
        long handle = this.handle;
        nativeDelete(handle);
        this.handle = 0;
    }

    /**
     * Similar to {@link BoxStore#runInTx(Runnable)}, but allows Tree functions.
     */
    public void runInTx(Runnable runnable) {
        store.runInTx(createTxRunnable(runnable));
    }

    /**
     * Similar to {@link BoxStore#runInReadTx(Runnable)}, but allows Tree functions.
     */
    public void runInReadTx(Runnable runnable) {
        store.runInReadTx(createTxRunnable(runnable));
    }

    /**
     * Similar to {@link BoxStore#callInReadTx(Callable)}, but allows Tree functions.
     */
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

    /**
     * Similar to {@link BoxStore#callInReadTx(Callable)}, but allows Tree functions.
     */
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

    /**
     * Puts (persists) a branch in the metamodel.
     */
    public long putMetaBranch(long id, long parentBranchId, String name) {
        return nativePutMetaBranch(handle, id, parentBranchId, name, null);
    }

    /**
     * Puts (persists) a branch in the metamodel with an optional description.
     */
    public long putMetaBranch(long id, long parentBranchId, String name, @Nullable String description) {
        return nativePutMetaBranch(handle, id, parentBranchId, name, description);
    }

    /**
     * Puts (persists) several branches in the metamodel to create the given path from the root.
     */
    public long[] putMetaBranches(String[] path) {
        return nativePutMetaBranches(handle, 0, path);
    }

    /**
     * Puts (persists) several branches in the metamodel from the given parent ID (must be a meta branch).
     */
    public long[] putMetaBranches(long parentBranchId, String[] path) {
        return nativePutMetaBranches(handle, parentBranchId, path);
    }

    /**
     * Puts (persists) a data leaf in the metamodel (describes values).
     */
    public long putMetaLeaf(long id, long parentBranchId, String name, short valueType) {
        return nativePutMetaLeaf(handle, id, parentBranchId, name, valueType, false, null);
    }

    /**
     * Puts (persists) a data leaf in the metamodel (describes values).
     */
    public long putMetaLeaf(long id, long parentBranchId, String name, short valueType,
                            boolean isUnsigned) {
        return nativePutMetaLeaf(handle, id, parentBranchId, name, valueType, isUnsigned, null);
    }

    /**
     * Puts (persists) a data leaf in the metamodel (describes values).
     */
    public long putMetaLeaf(long id, long parentBranchId, String name, short valueType,
                            boolean isUnsigned, @Nullable String description) {
        return nativePutMetaLeaf(handle, id, parentBranchId, name, valueType, isUnsigned, description);
    }

    /**
     * Put a new or existing data branch
     */
    public long putBranch(long id, long parentBranchId, long metaId, @Nullable String uid) {
        return nativePutBranch(handle, id, parentBranchId, metaId, uid);
    }

    /**
     * Put a new (inserts) data branch
     */
    public long putBranch(long parentBranchId, long metaId, @Nullable String uid) {
        return nativePutBranch(handle, 0, parentBranchId, metaId, uid);
    }

    /**
     * Put a new (inserts) data branch
     */
    public long putBranch(long parentBranchId, long metaId) {
        return nativePutBranch(handle, 0, parentBranchId, metaId, null);
    }


    /**
     * Puts (persists) a data value (using a data leaf). If a data leaf exists at the given ID, it's overwritten.
     *
     * @param id             Existing ID or zero to insert a new data leaf.
     * @param parentBranchId ID of the data branch, this data value belongs to.
     * @param metaId         ID of the metadata leaf "describing" this data value.
     * @param value          the actual data value.
     * @return the ID of the put data leaf.
     */
    public long putValue(long id, long parentBranchId, long metaId, long value) {
        return nativePutValueInteger(handle, id, parentBranchId, metaId, value);
    }

    /**
     * Puts (inserts) a new data value (using a data leaf).
     *
     * @param parentBranchId ID of the data branch, this data value belongs to.
     * @param metaId         ID of the metadata leaf "describing" this data value.
     * @param value          the actual data value.
     * @return the ID of the new data leaf.
     */
    public long putValue(long parentBranchId, long metaId, long value) {
        return nativePutValueInteger(handle, 0, parentBranchId, metaId, value);
    }

    /**
     * Puts (inserts) a new data value (using a data leaf).
     *
     * @param parentBranchId ID of the data branch, this data value belongs to.
     * @param metaId         ID of the metadata leaf "describing" this data value.
     * @param value          the actual data value.
     * @return the ID of the new data leaf.
     */
    public long putValue(long parentBranchId, long metaId, double value) {
        return nativePutValueFP(handle, 0, parentBranchId, metaId, value);
    }

    /**
     * Puts (persists) a data value (using a data leaf). If a data leaf exists at the given ID, it's overwritten.
     *
     * @param id             Existing ID or zero to insert a new data leaf.
     * @param parentBranchId ID of the data branch, this data value belongs to.
     * @param metaId         ID of the metadata leaf "describing" this data value.
     * @param value          the actual data value.
     * @return the ID of the put data leaf.
     */
    public long putValue(long id, long parentBranchId, long metaId, double value) {
        return nativePutValueFP(handle, id, parentBranchId, metaId, value);
    }

    /**
     * Puts (persists) a data value (using a data leaf). If a data leaf exists at the given ID, it's overwritten.
     *
     * @param id             Existing ID or zero to insert a new data leaf.
     * @param parentBranchId ID of the data branch, this data value belongs to.
     * @param metaId         ID of the metadata leaf "describing" this data value.
     * @param value          the actual data value.
     * @return the ID of the put data leaf.
     */
    public long putValue(long id, long parentBranchId, long metaId, String value) {
        return nativePutValueString(handle, id, parentBranchId, metaId, value);
    }

    /**
     * Puts (inserts) a new data value (using a data leaf).
     *
     * @param parentBranchId ID of the data branch, this data value belongs to.
     * @param metaId         ID of the metadata leaf "describing" this data value.
     * @param value          the actual data value.
     * @return the ID of the new data leaf.
     */
    public long putValue(long parentBranchId, long metaId, String value) {
        return nativePutValueString(handle, 0, parentBranchId, metaId, value);
    }

    /**
     * Puts (persists) a data leaf (containing a data value). If a data leaf exists with the same ID, it's overwritten.
     *
     * @return the ID of the put data leaf.
     */
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
