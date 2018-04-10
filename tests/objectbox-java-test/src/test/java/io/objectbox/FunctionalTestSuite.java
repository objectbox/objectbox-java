/*
 * Copyright 2017 ObjectBox Ltd. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
