package io.objectbox;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
        BoxStoreTest.class,
        CursorTest.class,
        CursorBytesTest.class,
        TransactionTest.class,
        BoxTest.class
})
public class FunctionalTestSuite {
}
