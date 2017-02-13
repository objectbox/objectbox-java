package io.objectbox;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import io.objectbox.query.LazyListTest;
import io.objectbox.query.QueryObserverTest;
import io.objectbox.query.QueryTest;

@RunWith(Suite.class)
@SuiteClasses({
        BoxTest.class,
        BoxStoreTest.class,
        BoxStoreBuilderTest.class,
        CursorTest.class,
        CursorBytesTest.class,
        LazyListTest.class,
        ObjectClassObserverTest.class,
        QueryObserverTest.class,
        QueryTest.class,
        TransactionTest.class,
})
public class FunctionalTestSuite {
}
