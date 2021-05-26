package io.objectbox.tree;

import io.objectbox.AbstractObjectBoxTest;
import io.objectbox.BoxStore;
import io.objectbox.model.PropertyType;
import org.junit.Before;
import org.junit.Test;

import static java.util.Objects.requireNonNull;
import static org.junit.Assert.*;

// TODO Add to FunctionalTestSuite.
public class TreeTest extends AbstractObjectBoxTest {
    private Tree tree;
    private Branch root;
    private long rootId;
    long[] metaBranchIds;

    @Override
    protected BoxStore createBoxStore() {
        return MyTreeModel.builder().build();
    }

    @Before
    public void createTree() {
        Tree prepTree = new Tree(store, 0);
        long rootId = prepTree.callInTxNoThrow(() -> {
            metaBranchIds = prepTree.putMetaBranches(new String[]{"Library", "Book", "Author"});
            return prepTree.putBranch( 0, metaBranchIds[0]);
        });
        tree = new Tree(store, rootId);
        root = tree.root();
        this.rootId = root.getId();
        assertNotEquals(0, this.rootId);
        assertEquals(3, metaBranchIds.length);
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
        tree.runInTx(() -> {
            // Meta
            long metaNameId = tree.putMetaLeaf(0, metaBranchIds[2], "Name", PropertyType.Short);
            assertNotEquals(0, metaNameId);

            // Data
            long libraryId = tree.putBranch(rootId, metaBranchIds[0]);
            assertNotEquals(0, libraryId);
            long bookId = tree.putBranch(libraryId, metaBranchIds[1]);
            assertNotEquals(0, bookId);
            long authorId = tree.putBranch(bookId, metaBranchIds[1]);
            assertNotEquals(0, authorId);
            tree.putValue(authorId, metaNameId, "Tolkien");

            // 2nd meta branch off from "Book"
            long[] metaBranchIds2 = tree.putMetaBranches(metaBranchIds[1], new String[]{"Publisher", "Company"});
            assertEquals(2, metaBranchIds2.length);
        });

        tree.runInReadTx(() -> {
            Branch book = root.branch("Book", true);
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
