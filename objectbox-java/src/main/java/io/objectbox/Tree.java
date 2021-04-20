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
