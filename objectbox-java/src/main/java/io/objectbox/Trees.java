package io.objectbox;

import javax.annotation.Nullable;

public class Trees {

    private BoxStore store;

    public Trees(BoxStore store) {
        this.store = store;
    }

    public DataBranch branch(String branchName, String branchUid) {
        throw new UnsupportedOperationException();
    }

    public static class DataBranch {

        public DataBranch branch(String[] path) {
            throw new UnsupportedOperationException();
        }

        public DataAttribute attribute(String[] path) {
            throw new UnsupportedOperationException();
        }

    }

    public static class DataAttribute {

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
