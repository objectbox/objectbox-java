package io.objectbox.tree;

import io.objectbox.BoxStore;

import javax.annotation.Nullable;

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
        this.handle = nativeCreate(store.getNativeStore(), uid);
    }

    public Branch root() {
        long dataBranchId = nativeRoot(handle);
        return new Branch(dataBranchId);
    }

    public void close() {
        long handle = this.handle;
        nativeDelete(handle);
        this.handle = 0;
    }

    /**
     * Create a (Data)Tree instance for the given meta-branch root, or find a singular root if 0 is given.
     */
    private static native long nativeCreate(long store, String uid);

    private static native void nativeDelete(long handle);

    /**
     * Get the root data branch ID.
     */
    private native long nativeRoot(long handle);

    public static class Branch {

        private final long id;

        Branch(long id) {
            this.id = id;
        }

        public Branch branch(String[] path) {
            throw new UnsupportedOperationException();
        }

        public Branch branch(String name) {
            return branch(new String[]{name});
        }

        public Leaf leaf(String[] path) {
            throw new UnsupportedOperationException();
        }

        public Leaf leaf(String name) {
            return leaf(new String[]{name});
        }

    }

    public static class Leaf {

        public boolean isInt() {
            throw new UnsupportedOperationException();
        }

        public boolean isDouble() {
            throw new UnsupportedOperationException();
        }

        public boolean isString() {
            throw new UnsupportedOperationException();
        }

        public boolean isStringArray() {
            throw new UnsupportedOperationException();
        }

        // valueInt
        @Nullable
        public Long asInt() {
            throw new UnsupportedOperationException();
        }

        // valueDouble
        @Nullable
        public Double asDouble() {
            throw new UnsupportedOperationException();
        }

        // valueString
        @Nullable
        public String asString() {
            throw new UnsupportedOperationException();
        }

        // valueStrings
        @Nullable
        public String[] asStringArray() {
            throw new UnsupportedOperationException();
        }

        public void setInt(@Nullable Long value) {
            throw new UnsupportedOperationException();
        }

        public void setDouble(@Nullable Double value) {
            throw new UnsupportedOperationException();
        }

        public void setString(@Nullable String value) {
            throw new UnsupportedOperationException();
        }

        public void setStringArray(@Nullable String[] value) {
            throw new UnsupportedOperationException();
        }

    }

}
