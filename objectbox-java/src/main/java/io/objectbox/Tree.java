package io.objectbox;

import javax.annotation.Nullable;

public class Tree {

    private final BoxStore store;
    @Nullable private final String uid;
    private long handle;

    public Tree(BoxStore store, @Nullable String uid) {
        this.store = store;
        this.uid = uid;
        this.handle = nativeCreate(uid);
    }

    public Branch root() {
        throw new UnsupportedOperationException();
    }

    private static native long nativeCreate(@Nullable String uid);

    public static class Branch {

        public Branch branch(String[] path) {
            throw new UnsupportedOperationException();
        }

        public Branch branch(String name) {
            throw new UnsupportedOperationException();
        }

        public Leaf leaf(String[] path) {
            throw new UnsupportedOperationException();
        }

        public Leaf leaf(String name) {
            throw new UnsupportedOperationException();
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
