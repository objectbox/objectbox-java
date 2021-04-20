package io.objectbox;

import javax.annotation.Nullable;

public class Tree {

    private BoxStore store;

    public Tree(BoxStore store) {
        this.store = store;
    }

    public Branch branch(String branchName, String branchUid) {
        throw new UnsupportedOperationException();
    }

    public static class Branch {

        public Branch branch(String[] path) {
            throw new UnsupportedOperationException();
        }

        public Leaf leaf(String[] path) {
            throw new UnsupportedOperationException();
        }

    }

    public static class Leaf {

        // TODO Add all supported types.

        public boolean isInt() {
            throw new UnsupportedOperationException();
        }

        public boolean isString() {
            throw new UnsupportedOperationException();
        }

        @Nullable
        public Long asInt() {
            throw new UnsupportedOperationException();
        }

        @Nullable
        public String asString() {
            throw new UnsupportedOperationException();
        }

        public void setInt(@Nullable Long value) {
            throw new UnsupportedOperationException();
        }

        public void setString(@Nullable String value) {
            throw new UnsupportedOperationException();
        }

    }

}
