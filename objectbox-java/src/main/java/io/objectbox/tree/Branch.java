package io.objectbox.tree;

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

    /**
     * Get the branch when following the given path starting from this branch.
     */
    public Branch branch(String[] path) {
        checkPath(path);
        long branchId = nativeBranch(tree.getHandle(), id, path);
        return new Branch(tree, branchId);
    }

    /**
     * Get the branch attached to this branch with the given name.
     */
    public Branch branch(String name) {
        checkName(name);
        return branch(new String[]{name});
    }

    /**
     * Get the leaf when following the given path starting from this branch.
     */
    public Leaf leaf(String[] path) {
        checkPath(path);
        LeafNode leafNode = nativeLeaf(tree.getHandle(), id, path);
        return new Leaf(leafNode);
    }

    /**
     * Get the leaf attached to this branch with the given name.
     */
    public Leaf leaf(String name) {
        checkName(name);
        return leaf(new String[]{name});
    }

    private void checkName(String name) {
        //noinspection ConstantConditions Nullability annotations are not enforced.
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("name must not be null or empty");
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
    private native long nativeBranch(long treeHandle, long branchId, String[] path);

    /**
     * Get a data leaf matching the path, starting at the given data {@code branchId}.
     * If {@code branchId == 0}, it assumes there's only one data tree in the database.
     */
    private native LeafNode nativeLeaf(long treeHandle, long branchId, String[] path);

}
