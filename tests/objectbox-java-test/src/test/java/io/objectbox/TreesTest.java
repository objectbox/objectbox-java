package io.objectbox;

import org.junit.Test;

// TODO Add to FunctionalTestSuite.
public class TreesTest {

    @Test
    public void trees_work() {
        BoxStore store = null;
        Trees trees = new Trees(store);

        // sub-tree
        Trees.DataBranch book = trees.branch("Book", "uid-4sdf6a4sdf6a4sdf64as6fd4");

        // branch
        Trees.DataBranch author = book.branch(new String[]{"Author"});
        Trees.DataAttribute nameIndirect = author.attribute(new String[]{"Name"});

        // attribute
        Trees.DataAttribute name = book.attribute(new String[]{"Author", "Name"});

        boolean isInt = name.isInt();
        boolean isString = name.isString();

        name.asInt();
        name.setInt(42L);
        String nameValue = name.asString();
        name.setString("Amy Blair");
    }
}
