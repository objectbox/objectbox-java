package io.objectbox.tree;

import io.objectbox.BoxStore;
import org.junit.Test;

// TODO Add to FunctionalTestSuite.
public class TreeTest {

    @Test
    public void trees_work() {
        BoxStore store = null;
        Tree tree = new Tree(store, "uid-4sdf6a4sdf6a4sdf64as6fd4");

        // get tree root
        Tree.Branch book = tree.root();

        // get leaf indirectly by traversing branches
        Tree.Branch author = book.branch("Author");
        Tree.Leaf nameIndirect = author.leaf("Name");

        // get leaf directly
        Tree.Leaf name = book.leaf(new String[]{"Author", "Name"});

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
    }
}
