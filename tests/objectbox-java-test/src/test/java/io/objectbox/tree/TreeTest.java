package io.objectbox.tree;

import io.objectbox.AbstractObjectBoxTest;
import io.objectbox.BoxStore;
import io.objectbox.model.PropertyType;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.Objects.requireNonNull;
import static org.junit.Assert.*;

public class TreeTest extends AbstractObjectBoxTest {
    private Tree tree;
    private Branch root;
    private long rootId;
    private long[] metaBranchIds;

    @Override
    protected BoxStore createBoxStore() {
        return MyTreeModel.builder().build();
    }

    @Before
    public void createTree() {
        Tree prepTree = new Tree(store, 0);
        long rootId = prepTree.callInTxNoThrow(() -> {
            metaBranchIds = prepTree.putMetaBranches(new String[]{"Library", "Book", "Author"});
            return prepTree.putBranch(0, metaBranchIds[0]);  // Library data branch (data tree root)
        });
        tree = new Tree(store, rootId);
        root = tree.getRoot();
        this.rootId = root.getId();
        assertNotEquals(0, this.rootId);
        assertEquals(rootId, tree.getRootId());
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

        // Note: this is somewhat in the gray zone as we do not set up corresponding a meta tree
        tree.runInTx(() -> {
            intId[0] = tree.putValue(0, rootId, 0, 42);
            doubleId[0] = tree.putValue(0, rootId, 0, 3.141);
            stringId[0] = tree.putValue(0, rootId, 0, "foo-tree");
        });

        assertNotEquals(0, intId[0]);
        assertNotEquals(0, doubleId[0]);
        assertNotEquals(0, stringId[0]);

        tree.runInReadTx(() -> {
            Leaf intLeaf = tree.getLeaf(intId[0]);
            assertNotNull(intLeaf);
            assertEquals(42, intLeaf.getInt());
            assertTrue(intLeaf.isInt());
            assertFalse(intLeaf.isDouble());
            assertFalse(intLeaf.isString());
            assertFalse(intLeaf.isStringArray());

            assertEquals("42", intLeaf.asString());
            assertEquals(42.0, requireNonNull(intLeaf.asDouble()), 0.0);
            assertArrayEquals(new String[]{"42"}, intLeaf.asStringArray());

            assertEquals(Long.valueOf(42), tree.getInteger(intId[0]));

            assertEquals(3.141, requireNonNull(tree.getLeaf(doubleId[0])).getDouble(), 0.0);
            assertEquals(Double.valueOf(3.141), tree.getDouble(doubleId[0]));

            assertEquals("foo-tree", requireNonNull(tree.getLeaf(stringId[0])).getString());
            assertEquals("foo-tree", tree.getString(stringId[0]));
        });
    }

    @Test
    public void treePath() {
        long[] metaLeafIds = {0};

        tree.runInTx(() -> {
            // Meta
            long metaNameId = tree.putMetaLeaf(0, metaBranchIds[2], "Name", PropertyType.String);
            long metaYearId = tree.putMetaLeaf(0, metaBranchIds[2], "Year", PropertyType.Int);
            long metaPriceId = tree.putMetaLeaf(0, metaBranchIds[2], "Height", PropertyType.Double);
            assertNotEquals(0, metaNameId);
            assertNotEquals(0, metaYearId);
            assertNotEquals(0, metaPriceId);
            metaLeafIds[0] = metaNameId;

            // Data
            long libraryId = rootId;
            assertNotEquals(0, libraryId);
            long bookId = tree.putBranch(libraryId, metaBranchIds[1]);
            assertNotEquals(0, bookId);
            long authorId = tree.putBranch(bookId, metaBranchIds[2]);
            assertNotEquals(0, authorId);
            tree.putValue(authorId, metaNameId, "Tolkien");
            tree.putValue(authorId, metaYearId, 2021);
            tree.putValue(authorId, metaPriceId, 12.34);

            // 2nd meta branch off from "Book"
            long[] metaBranchIds2 = tree.putMetaBranches(metaBranchIds[1], new String[]{"Publisher", "Company"});
            assertEquals(2, metaBranchIds2.length);
        });

        tree.runInReadTx(() -> {
            Branch book = root.branch("Book");
            assertNotNull(book);
            // get leaf indirectly by traversing branches
            Branch author = book.branchChild("Author");
            assertNotNull(author);

            Leaf name = author.leafChild("Name");
            assertNotNull(name);
            assertEquals("Tolkien", name.getString());
            assertFalse(name.isInt());
            assertFalse(name.isDouble());
            assertTrue(name.isString());
            assertFalse(name.isStringArray());
            assertNotEquals(0, name.getId());
            assertEquals(author.getId(), name.getParentBranchId());
            assertEquals(metaLeafIds[0], name.getMetaId());

            Leaf year = author.leafChild("Year");
            assertNotNull(year);
            assertEquals(2021, year.getInt());

            Leaf height = author.leafChild("Height");
            assertNotNull(height);
            assertEquals(12.34, height.getDouble(), 0.0);

            // get leaf directly
            Leaf name2 = book.leaf(new String[]{"Author", "Name"});
            assertNotNull(name2);
            assertEquals("Tolkien", name2.getString());

            // get leaf directly via path string
            name2 = book.leaf("Author.Name");
            assertNotNull(name2);
            assertEquals("Tolkien", name2.getString());

            // get leaf directly via path string with another separator
            assertNull(book.leaf("Author/Name"));
            tree.setPathSeparatorRegex("\\/");
            name2 = book.leaf("Author/Name");
            assertNotNull(name2);
            assertEquals("Tolkien", name2.getString());

        });
    }

    @Test
    public void putValueForExistingLeaf_String() {
        tree.runInTx(() -> {
            long metaNameId = tree.putMetaLeaf(0, metaBranchIds[0], "Name", PropertyType.String);
            assertNotEquals(0, metaNameId);
            tree.putValue(rootId, metaNameId, "Bookery");
            Leaf leaf = root.leafChild("Name");
            assertNotNull(leaf);
            assertEquals("Bookery", leaf.getString());

            assertThrows(IllegalStateException.class, () -> leaf.setInt(42));
            assertThrows(IllegalStateException.class, () -> leaf.setDouble(3.141));
            assertThrows(IllegalStateException.class, () -> leaf.setStringArray(new String[]{}));

            leaf.setString("Unseen Library");
            long idPut = tree.put(leaf);
            assertEquals(leaf.getId(), idPut); // Unchanged
        });

        tree.runInReadTx(() -> {
            Leaf name = root.leafChild("Name");
            assertNotNull(name);
            assertEquals("Unseen Library", name.getString());
        });
    }

    @Test
    public void putValueForExistingLeaf_Int() {
        tree.runInTx(() -> {
            long metaYearId = tree.putMetaLeaf(0, metaBranchIds[0], "Year", PropertyType.Int);
            assertNotEquals(0, metaYearId);
            tree.putValue(rootId, metaYearId, 1982);
            Leaf leaf = root.leafChild("Year");
            assertNotNull(leaf);
            assertEquals(1982, leaf.getInt());

            assertThrows(IllegalStateException.class, () -> leaf.setString("foo"));
            assertThrows(IllegalStateException.class, () -> leaf.setDouble(3.141));
            assertThrows(IllegalStateException.class, () -> leaf.setStringArray(new String[]{}));

            leaf.setInt(1977);
            long idPut = tree.put(leaf);
            assertEquals(leaf.getId(), idPut); // Unchanged
        });

        tree.runInReadTx(() -> {
            Leaf year = root.leafChild("Year");
            assertNotNull(year);
            assertEquals(1977, year.getInt());
        });
    }

    @Test
    public void concurrentTxs() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(3);
        final AtomicBoolean readThreadOK = new AtomicBoolean(false);
        final AtomicLong bookBranchId = new AtomicLong(0);
        Thread readThread = new Thread(() -> {
            tree.runInReadTx(() -> {
                System.out.println("Thread " + Thread.currentThread().getId() + " entered tree TX");
                latch.countDown();
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                assertNull(tree.getRoot().branchChild("Book"));
                readThreadOK.set(true);
            });
        });
        readThread.start();

        Thread writeThread = new Thread(() -> {
            tree.runInTx(() -> {
                System.out.println("Thread " + Thread.currentThread().getId() + " entered tree TX (write)");
                latch.countDown();
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                long id = tree.putBranch(tree.getRoot().getId(), metaBranchIds[1]);
                bookBranchId.set(id);
            });
        });
        writeThread.start();

        Callable<Branch> branchCallable = () -> {
            System.out.println("Thread " + Thread.currentThread().getId() + " entered tree TX");
            latch.countDown();
            latch.await();
            return tree.getRoot().branchChild("Book");
        };
        Branch branch = tree.callInReadTx(branchCallable);
        assertNull(branch);

        // And once more to see that read TXs can still be started
        tree.callInReadTx(branchCallable);

        readThread.join();
        assertTrue(readThreadOK.get());
        writeThread.join();
        assertNotEquals(0, bookBranchId.get());

        branch = tree.callInReadTx(branchCallable);
        assertNotNull(branch);
        assertEquals(bookBranchId.get(), branch.getId());
    }
}
