package io.objectbox;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import io.objectbox.index.IndexReaderRenewTest;
import io.objectbox.query.LazyListTest;
import io.objectbox.query.QueryObserverTest;
import io.objectbox.query.QueryTest;
import io.objectbox.relation.RelationEagerTest;
import io.objectbox.relation.RelationTest;
import io.objectbox.relation.ToOneTest;

@RunWith(Suite.class)
@SuiteClasses({
//NOTE: there is a duplicate class (used by Gradle) where any change must be applied too: see src/test/...
        BoxTest.class,
        BoxStoreTest.class,
        BoxStoreBuilderTest.class,
        CursorTest.class,
        CursorBytesTest.class,
        LazyListTest.class,
        NonArgConstructorTest.class,
        IndexReaderRenewTest.class,
        ObjectClassObserverTest.class,
        QueryObserverTest.class,
        QueryTest.class,
        RelationTest.class,
        RelationEagerTest.class,
        ToOneTest.class,
        TransactionTest.class,
})
public class FunctionalTestSuite {
}
