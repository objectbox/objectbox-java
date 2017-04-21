package io.objectbox;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import io.objectbox.index.IndexReaderRenewTest;
import io.objectbox.query.LazyListTest;
import io.objectbox.query.QueryObserverTest;
import io.objectbox.query.QueryTest;
import io.objectbox.relation.RelationTest;
import io.objectbox.relation.ToOneTest;

@RunWith(Suite.class)
@SuiteClasses({
        BoxTest.class,
        BoxStoreTest.class,
        BoxStoreBuilderTest.class,
        CursorTest.class,
        CursorBytesTest.class,
        LazyListTest.class,
        IndexReaderRenewTest.class,
        ObjectClassObserverTest.class,
        QueryObserverTest.class,
        QueryTest.class,
        RelationTest.class,
        ToOneTest.class,
        TransactionTest.class,
})
public class FunctionalTestSuite {
}
