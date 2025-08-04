/*
 * Copyright 2017 ObjectBox Ltd.
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

package io.objectbox.query;

import io.objectbox.query.QueryBuilder.StringOrder;
import org.junit.Test;

import java.util.List;

import io.objectbox.TestEntity;
import io.objectbox.TestEntity_;


import static io.objectbox.TestEntity_.simpleInt;
import static io.objectbox.TestEntity_.simpleLong;
import static org.junit.Assert.assertEquals;

/**
 * Tests {@link Query} using the new query builder API with nesting support.
 */
public class QueryTest2 extends AbstractQueryTest {

    @Test
    public void newQueryApi() {
        putTestEntity("Fry", 14);
        putTestEntity("Fry", 12);
        putTestEntity("Fry", 10);

        // current query API
        Query<TestEntity> query = box.query()
                .equal(TestEntity_.simpleString, "Fry", StringOrder.CASE_INSENSITIVE)
                .less(TestEntity_.simpleInt, 12)
                .or()
                .in(TestEntity_.simpleLong, new long[]{1012})
                .order(TestEntity_.simpleInt)
                .build();

        List<TestEntity> results = query.find();
        assertEquals(2, results.size());
        assertEquals(10, results.get(0).getSimpleInt());
        assertEquals(12, results.get(1).getSimpleInt());

        // suggested query API
        Query<TestEntity> newQuery = box.query(
                TestEntity_.simpleString.equal("Fry")
                        .and(TestEntity_.simpleInt.less(12)
                                .or(TestEntity_.simpleLong.oneOf(new long[]{1012}))))
                .order(TestEntity_.simpleInt)
                .build();

        List<TestEntity> newResults = newQuery.find();
        assertEquals(2, newResults.size());
        assertEquals(10, newResults.get(0).getSimpleInt());
        assertEquals(12, newResults.get(1).getSimpleInt());
    }

    /**
     * Combining conditions using and(cond) and or(cond) implicitly adds parentheses.
     */
    @Test
    public void combiningAddsImplicitParentheses() {
        putTestEntity("Fry", 14);
        putTestEntity("Fry", 12);
        putTestEntity("Fry", 10);

        // Nested OR
        // (Sarah OR Fry) AND <12
        List<TestEntity> resultsOr = box.query(
                TestEntity_.simpleString.equal("Sarah")
                        .or(
                                TestEntity_.simpleString.equal("Fry")
                        )
                        .and(
                                TestEntity_.simpleInt.less(12)
                        )
        ).build().find();
        // <12 AND (Fry OR Sarah)
        List<TestEntity> resultsNestedOr = box.query(
                TestEntity_.simpleInt.less(12)
                        .and(
                                TestEntity_.simpleString.equal("Fry")
                                        .or(
                                                TestEntity_.simpleString.equal("Sarah")
                                        )
                        )
        ).build().find();
        // Only the Fry age 10.
        assertEquals(1, resultsOr.size());
        assertEquals(1, resultsNestedOr.size());
        assertEquals(10, resultsOr.get(0).getSimpleInt());
        assertEquals(10, resultsNestedOr.get(0).getSimpleInt());

        // Nested AND
        // (<12 AND Sarah) OR Fry
        List<TestEntity> resultsAnd = box.query(
                TestEntity_.simpleInt.less(12)
                        .and(
                                TestEntity_.simpleString.equal("Sarah")
                        )
                        .or(
                                TestEntity_.simpleString.equal("Fry")
                        )
        ).build().find();
        // Fry OR (Sarah AND <12)
        List<TestEntity> resultsNestedAnd = box.query(
                TestEntity_.simpleString.equal("Fry")
                        .or(
                                TestEntity_.simpleString.equal("Sarah")
                                        .and(
                                                TestEntity_.simpleInt.less(12)
                                        )
                        )
        ).build().find();
        // All three Fry's.
        assertEquals(3, resultsAnd.size());
        assertEquals(3, resultsNestedAnd.size());
    }

    @Test
    public void or() {
        putTestEntitiesScalars();
        Query<TestEntity> query = box.query(simpleInt.equal(2007).or(simpleLong.equal(3002))).build();
        List<TestEntity> entities = query.find();
        assertEquals(2, entities.size());
        assertEquals(3002, entities.get(0).getSimpleLong());
        assertEquals(2007, entities.get(1).getSimpleInt());
    }

    @Test
    public void and() {
        putTestEntitiesScalars();
        // Result if OR precedence (wrong): {}, AND precedence (expected): {2008}
        Query<TestEntity> query = box.query(TestEntity_.simpleInt.equal(2006)
                .and(TestEntity_.simpleInt.equal(2007))
                .or(TestEntity_.simpleInt.equal(2008)))
                .build();
        List<TestEntity> entities = query.find();
        assertEquals(1, entities.size());
        assertEquals(2008, entities.get(0).getSimpleInt());
    }

    /**
     * https://github.com/objectbox/objectbox-java/issues/834
     */
    @Test
    public void parameterAlias_combineWithOr() {
        putTestEntitiesScalars();

        Query<TestEntity> query = box.query(
                simpleInt.greater(0).alias("greater")
                        .or(simpleInt.less(0).alias("less"))
        ).order(simpleInt).build();
        List<TestEntity> results = query
                .setParameter("greater", 2008)
                .setParameter("less", 2001)
                .find();
        assertEquals(2, results.size());
        assertEquals(2000, results.get(0).getSimpleInt());
        assertEquals(2009, results.get(1).getSimpleInt());
    }

}
