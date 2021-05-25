package io.objectbox.tree;

import io.objectbox.AbstractObjectBoxTest;
import io.objectbox.BoxStore;
import org.junit.Before;
import org.junit.Test;

import static java.util.Objects.requireNonNull;
import static org.junit.Assert.*;

// TODO Add to FunctionalTestSuite.
public class TreeTest extends AbstractObjectBoxTest {
    private Tree tree;
    private Branch root;
    private long rootId;

    @Override
    protected BoxStore createBoxStore() {
        return MyTreeModel.builder().build();
    }

    @Before
    public void createTree() {
        Tree emptyTree = new Tree(store, 0);
        long rootId = emptyTree.callInTxNoThrow(() -> emptyTree.putBranch(0, 0, 0, null));
        tree = new Tree(store, rootId);
        root = tree.root();
        this.rootId = root.getId();
        assertNotEquals(0, this.rootId);
    }

    @Test(expected = IllegalStateException.class)
    public void putWithoutTx() {
        tree.putBranch(0, rootId, 0, null);
    }

    @Test
    public void getNotFound() {
        tree.runInReadTx(() -> {
            assertNull(tree.getLeaf(42));
            assertNull(tree.getString(42));
            assertNull(tree.getInteger(42));
            assertNull(tree.getDouble(42));
        });
    }

    @Test
    public void putAndGetValue() {
        long[] intId = {0}, doubleId = {0}, stringId = {0};  // Use arrays to allow assigning inside lambda

        tree.runInTx(() -> {
            intId[0] = tree.putValue(0, rootId, 0, 42);
            doubleId[0] = tree.putValue(0, rootId, 0, 3.141);
            stringId[0] = tree.putValue(0, rootId, 0, "foo-tree");
        });

        assertNotEquals(0, intId[0]);
        assertNotEquals(0, doubleId[0]);
        assertNotEquals(0, stringId[0]);

        tree.runInReadTx(() -> {
            assertEquals(Long.valueOf(42), requireNonNull(tree.getLeaf(intId[0])).getInt());
            assertEquals(Long.valueOf(42), tree.getInteger(intId[0]));

            assertEquals(Double.valueOf(3.141), requireNonNull(tree.getLeaf(doubleId[0])).getDouble());
            assertEquals(Double.valueOf(3.141), tree.getDouble(doubleId[0]));

            assertEquals("foo-tree", requireNonNull(tree.getLeaf(stringId[0])).getString());
            assertEquals("foo-tree", tree.getString(stringId[0]));
        });
    }

    @Test
    public void treePath() {
        Branch book = root;

        tree.runInTx(() -> {
            // get leaf indirectly by traversing branches
            Branch author = book.branch("Author");
            Leaf nameIndirect = author.leaf("Name");

            // get leaf directly
            Leaf name = book.leaf(new String[]{"Author", "Name"});

            boolean isInt = name.isInt();
            boolean isDouble = name.isDouble();
            boolean isString = name.isString();
            boolean isStringArray = name.isStringArray();

            Long aLong = name.asInt();
            Double aDouble = name.asDouble();
            String string = name.asString();
            String[] strings = name.asStringArray();

            name.setInt(42L);
            name.setDouble(21.0);
            name.setString("Amy Blair");
            name.setStringArray(new String[]{"Amy", "Blair"});
        });
    }

}
