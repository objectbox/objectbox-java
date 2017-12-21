package io.objectbox.annotation;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import io.objectbox.BoxStoreBuilderTest;
import io.objectbox.BoxStoreTest;
import io.objectbox.BoxTest;
import io.objectbox.CursorBytesTest;
import io.objectbox.CursorTest;
import io.objectbox.NonArgConstructorTest;
import io.objectbox.ObjectClassObserverTest;
import io.objectbox.TransactionTest;
import io.objectbox.index.IndexReaderRenewTest;
import io.objectbox.query.LazyListTest;
import io.objectbox.query.QueryObserverTest;
import io.objectbox.query.PropertyQueryTest;
import io.objectbox.query.QueryTest;
import io.objectbox.relation.RelationEagerTest;
import io.objectbox.relation.RelationTest;
import io.objectbox.relation.ToOneTest;

/** Duplicate for gradle */
@RunWith(Suite.class)
@SuiteClasses({
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
        PropertyQueryTest.class,
        RelationTest.class,
        RelationEagerTest.class,
        ToOneTest.class,
        TransactionTest.class,
})
public class FunctionalTestSuite {
}
