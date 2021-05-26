package io.objectbox.tree;

import javax.annotation.Nullable;

/**
 * A branch within a {@link Tree}. May have {@link #branch(String[]) branches} or {@link #leaf(String[]) leaves}.
 */
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
     * Get the branch when following the given path starting from this branch.
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
     * Get the branch attached to this branch with the given name or
     * if {@code isDotSeparatedPath} the branch when following the path
     * (e.g. {@code Branch1.Branch2}) starting from this branch.
     * @return null if no matching tree node was found
     */
    @Nullable
    public Branch branch(String nameOrDotPath, boolean isDotSeparatedPath) {
        checkNameOrDotPath(nameOrDotPath);
        String[] path;
        if (isDotSeparatedPath) {
            path = nameOrDotPath.split("\\.");
        } else {
            path = new String[]{nameOrDotPath};
        }
        return branch(path);
    }

    /**
     * Get the branch attached to this branch with the given name.
     * @return null if no matching tree node was found
     */
    @Nullable
    public Branch branch(String name) {
        return branch(name, false);
    }

    /**
     * Get the leaf when following the given path starting from this branch.
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
     * Get the leaf attached to this branch with the given name or
     * if {@code isDotSeparatedPath} the leaf when following the path
     * (e.g. {@code Branch1.Leaf1}) starting from this branch.
     * @return null if no matching tree node was found
     */
    @Nullable
    public Leaf leaf(String nameOrDotPath, boolean isDotSeparatedPath) {
        checkNameOrDotPath(nameOrDotPath);
        String[] path;
        if (isDotSeparatedPath) {
            path = nameOrDotPath.split("\\.");
        } else {
            path = new String[]{nameOrDotPath};
        }
        return leaf(path);
    }

    /**
     * Get the leaf attached to this branch with the given name.
     * @return null if no matching tree node was found
     */
    @Nullable
    public Leaf leaf(String name) {
        return leaf(name, false);
    }

    private void checkNameOrDotPath(String name) {
        //noinspection ConstantConditions Nullability annotations are not enforced.
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("nameOrDotPath must not be null or empty");
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
