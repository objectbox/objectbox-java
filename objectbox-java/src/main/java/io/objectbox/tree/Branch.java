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

import javax.annotation.Nullable;

import io.objectbox.annotation.apihint.Experimental;

/**
 * A branch within a {@link Tree}. May have {@link #branch(String[]) branches} or {@link #leaf(String[]) leaves}.
 */
@Experimental
public class Branch {

    private final Tree tree;
    private final long id;

    Branch(Tree tree, long id) {
        this.tree = tree;
        this.id = id;
    }

    public Tree getTree() {
        return tree;
    }

    public long getId() {
        return id;
    }

    /**
     * Get the branch following the given path of child branches from this branch.
     *
     * @return null if no matching tree node was found
     */
    @Nullable
    public Branch branch(String[] path) {
        checkPath(path);
        long branchId = nativeGetBranchId(tree.getHandle(), id, path);
        if (branchId == 0) return null;
        return new Branch(tree, branchId);
    }

    /**
     * Get the branch following the given path of child branches from this branch.
     *
     * @return null if no matching tree node was found
     */
    @Nullable
    public Branch branch(String pathString) {
        checkNameOrPath(pathString);
        String[] path = pathString.split(tree.getPathSeparatorRegex());
        return branch(path);
    }

    /**
     * Get the child branch directly attached to this branch with the given name.
     *
     * @return null if no matching tree node was found
     */
    @Nullable
    public Branch branchChild(String name) {
        String[] path = new String[]{name};
        return branch(path);
    }

    /**
     * Get the leaf following the given path of children from this branch.
     *
     * @return null if no matching tree node was found
     */
    @Nullable
    public Leaf leaf(String[] path) {
        checkPath(path);
        LeafNode leafNode = nativeGetLeaf(tree.getHandle(), id, path);
        if (leafNode == null) return null;
        return new Leaf(leafNode);
    }

    /**
     * Get the leaf following the given path of children from this branch.
     *
     * @return null if no matching tree node was found
     */
    @Nullable
    public Leaf leaf(String pathString) {
        checkNameOrPath(pathString);
        String[] path = pathString.split(tree.getPathSeparatorRegex());
        return leaf(path);
    }

    /**
     * Get the child leaf directly attached to this branch with the given name.
     *
     * @return null if no matching tree node was found
     */
    @Nullable
    public Leaf leafChild(String name) {
        checkNameOrPath(name);
        String[] path = new String[]{name};
        return leaf(path);
    }

    private void checkNameOrPath(String name) {
        //noinspection ConstantConditions Nullability annotations are not enforced.
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("name/path must not be null or empty");
        }
    }

    private void checkPath(String[] path) {
        //noinspection ConstantConditions Nullability annotations are not enforced.
        if (path == null || path.length == 0) {
            throw new IllegalArgumentException("path must not be null or empty");
        }
    }

    /**
     * Get a data branch ID matching the path, starting at the given data {@code branchId}.
     * If {@code branchId == 0}, it assumes there's only one data tree in the database.
     */
    private native long nativeGetBranchId(long treeHandle, long branchId, String[] path);

    /**
     * Get a data leaf matching the path, starting at the given data {@code branchId}.
     * If {@code branchId == 0}, it assumes there's only one data tree in the database.
     */
    private native LeafNode nativeGetLeaf(long treeHandle, long branchId, String[] path);

}
