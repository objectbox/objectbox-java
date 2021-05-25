package io.objectbox.tree;

import io.objectbox.AbstractObjectBoxTest;
import io.objectbox.BoxStore;
import org.junit.Test;

// TODO Add to FunctionalTestSuite.
public class TreeTest extends AbstractObjectBoxTest {
    @Override
    protected BoxStore createBoxStore() {
        return MyTreeModel.builder().build();
    }

    @Test
    public void trees_work() {
        Tree tree = new Tree(store, 0);

        // get tree root
        Branch book = tree.root();

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
