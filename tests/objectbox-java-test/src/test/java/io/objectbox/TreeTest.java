package io.objectbox;

import org.junit.Test;

// TODO Add to FunctionalTestSuite.
public class TreeTest {

    @Test
    public void trees_work() {
        BoxStore store = null;
        Tree tree = new Tree(store);

        // sub-tree
        Tree.Branch book = tree.branch("Book", "uid-4sdf6a4sdf6a4sdf64as6fd4");

        // branch
        Tree.Branch author = book.branch(new String[]{"Author"});
        Tree.Leaf nameIndirect = author.leaf(new String[]{"Name"});

        // attribute
        Tree.Leaf name = book.leaf(new String[]{"Author", "Name"});

        boolean isInt = name.isInt();
        boolean isString = name.isString();

        name.asInt();
        name.setInt(42L);
        String nameValue = name.asString();
        name.setString("Amy Blair");
    }
}
