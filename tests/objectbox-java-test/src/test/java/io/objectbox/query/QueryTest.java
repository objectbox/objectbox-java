/*
 * Copyright © 2016 ObjectBox Ltd. <https://objectbox.io/>
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

import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import io.objectbox.Box;
import io.objectbox.BoxStore;
import io.objectbox.BoxStoreBuilder;
import io.objectbox.TestEntity;
import io.objectbox.TestEntity_;
import io.objectbox.TestUtils;
import io.objectbox.exception.DbExceptionListener;
import io.objectbox.exception.NonUniqueResultException;
import io.objectbox.query.QueryBuilder.StringOrder;


import static io.objectbox.TestEntity_.date;
import static io.objectbox.TestEntity_.simpleBoolean;
import static io.objectbox.TestEntity_.simpleByteArray;
import static io.objectbox.TestEntity_.simpleFloat;
import static io.objectbox.TestEntity_.simpleInt;
import static io.objectbox.TestEntity_.simpleLong;
import static io.objectbox.TestEntity_.simpleShort;
import static io.objectbox.TestEntity_.simpleString;
import static io.objectbox.TestEntity_.simpleStringArray;
import static io.objectbox.TestEntity_.stringObjectMap;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class QueryTest extends AbstractQueryTest {

    @Test
    public void createIfStoreClosed_throws() {
        store.close();

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> box.query()
        );
        assertEquals("Store is closed", ex.getMessage());
    }

    @Test
    public void testBuild() {
        try (Query<TestEntity> query = box.query().build()) {
            assertNotNull(query);
        }
    }

    @Test
    public void useAfterBuild_fails() {
        QueryBuilder<TestEntity> builder = box.query();
        Query<TestEntity> query = builder.build();

        // Calling any builder method after build should fail.
        // note: not calling all variants for different types.
        assertThrowsBuilderClosed(() -> builder.isNull(TestEntity_.simpleString));
        assertThrowsBuilderClosed(builder::and);
        assertThrowsBuilderClosed(() -> builder.notNull(TestEntity_.simpleString));
        assertThrowsBuilderClosed(builder::or);
        assertThrowsBuilderClosed(() -> builder.equal(TestEntity_.simpleBoolean, true));
        assertThrowsBuilderClosed(() -> builder.notEqual(TestEntity_.simpleBoolean, true));
        assertThrowsBuilderClosed(() -> builder.less(TestEntity_.simpleInt, 42));
        assertThrowsBuilderClosed(() -> builder.greater(TestEntity_.simpleInt, 42));
        assertThrowsBuilderClosed(() -> builder.between(TestEntity_.simpleInt, 42, 43));
        assertThrowsBuilderClosed(() -> builder.in(TestEntity_.simpleInt, new int[]{42}));
        assertThrowsBuilderClosed(() -> builder.notIn(TestEntity_.simpleInt, new int[]{42}));
        assertThrowsBuilderClosed(() -> builder.contains(TestEntity_.simpleString, "42", StringOrder.CASE_INSENSITIVE));
        assertThrowsBuilderClosed(() -> builder.startsWith(TestEntity_.simpleString, "42", StringOrder.CASE_SENSITIVE));
        assertThrowsBuilderClosed(() -> builder.order(TestEntity_.simpleInt));
        assertThrowsBuilderClosed(builder::build);

        query.close();
    }

    private void assertThrowsBuilderClosed(ThrowingRunnable runnable) {
        IllegalStateException ex = assertThrows(IllegalStateException.class, runnable);
        assertEquals("This QueryBuilder has already been closed. Please use a new instance.", ex.getMessage());
    }

    @Test
    public void useAfterQueryClose_fails() {
        Query<TestEntity> query = box.query().build();
        query.close();

        assertThrowsQueryIsClosed(query::count);
        assertThrowsQueryIsClosed(query::describe);
        assertThrowsQueryIsClosed(query::describeParameters);
        assertThrowsQueryIsClosed(query::findFirst);
        assertThrowsQueryIsClosed(query::findUnique);
        assertThrowsQueryIsClosed(query::find);
        assertThrowsQueryIsClosed(() -> query.find(0, 1));
        assertThrowsQueryIsClosed(query::findFirstId);
        assertThrowsQueryIsClosed(query::findUniqueId);
        assertThrowsQueryIsClosed(query::findIds);
        assertThrowsQueryIsClosed(() -> query.findIds(0, 1));
        assertThrowsQueryIsClosed(query::findLazy);
        assertThrowsQueryIsClosed(query::findLazyCached);
        assertThrowsQueryIsClosed(query::remove);

        // For setParameter(s) the native method is not actually called, so fine to use incorrect alias and property.
        assertThrowsQueryIsClosed(() -> query.setParameter("none", "value"));
        assertThrowsQueryIsClosed(() -> query.setParameters("none", "a", "b"));
        assertThrowsQueryIsClosed(() -> query.setParameter("none", 1));
        assertThrowsQueryIsClosed(() -> query.setParameter("none", new int[]{1, 2}));
        assertThrowsQueryIsClosed(() -> query.setParameter("none", new long[]{1, 2}));
        assertThrowsQueryIsClosed(() -> query.setParameters("none", 1, 2));
        assertThrowsQueryIsClosed(() -> query.setParameter("none", 1.0));
        assertThrowsQueryIsClosed(() -> query.setParameters("none", 1.0, 2.0));
        assertThrowsQueryIsClosed(() -> query.setParameter("none", new String[]{"a", "b"}));
        assertThrowsQueryIsClosed(() -> query.setParameter("none", new byte[]{1, 2}));
        assertThrowsQueryIsClosed(() -> query.setParameter(simpleString, "value"));
        assertThrowsQueryIsClosed(() -> query.setParameters(simpleString, "a", "b"));
        assertThrowsQueryIsClosed(() -> query.setParameter(simpleString, 1));
        assertThrowsQueryIsClosed(() -> query.setParameter(simpleString, new int[]{1, 2}));
        assertThrowsQueryIsClosed(() -> query.setParameter(simpleString, new long[]{1, 2}));
        assertThrowsQueryIsClosed(() -> query.setParameters(simpleString, 1, 2));
        assertThrowsQueryIsClosed(() -> query.setParameter(simpleString, 1.0));
        assertThrowsQueryIsClosed(() -> query.setParameters(simpleString, 1.0, 2.0));
        assertThrowsQueryIsClosed(() -> query.setParameter(simpleString, new String[]{"a", "b"}));
        assertThrowsQueryIsClosed(() -> query.setParameter(simpleString, new byte[]{1, 2}));

        // find would throw once first results are obtained, but shouldn't allow creating an observer to begin with.
        assertThrowsQueryIsClosed(() -> query.subscribe().observer(data -> {
        }));
    }

    private void assertThrowsQueryIsClosed(ThrowingRunnable runnable) {
        IllegalStateException ex = assertThrows(IllegalStateException.class, runnable);
        assertEquals("This query is closed. Build and use a new one.", ex.getMessage());
    }

    @Test
    public void useAfterStoreClose_failsIfUsingStore() {
        Query<TestEntity> query = box.query(
                simpleString.equal("")
                        .and(stringObjectMap.equalKeyValue("", "", StringOrder.CASE_SENSITIVE))
                        .and(simpleInt.equal(0))
                        .and(simpleInt.oneOf(new int[]{0}).alias("oneOf4"))
                        .and(simpleLong.oneOf(new long[]{0}).alias("oneOf8"))
                        .and(simpleInt.between(0, 0).alias("between"))
                        .and(simpleString.oneOf(new String[]{""}).alias("oneOfS"))
                        .and(simpleByteArray.equal(new byte[]{0}))
        ).build();
        store.close();

        // All methods accessing the store throw.
        assertThrowsStoreIsClosed(query::count);
        assertThrowsStoreIsClosed(query::findFirst);
        assertThrowsStoreIsClosed(query::findUnique);
        assertThrowsStoreIsClosed(query::find);
        assertThrowsStoreIsClosed(() -> query.find(0, 1));
        assertThrowsStoreIsClosed(query::findFirstId);
        assertThrowsStoreIsClosed(query::findUniqueId);
        assertThrowsStoreIsClosed(query::findIds);
        assertThrowsStoreIsClosed(() -> query.findIds(0, 1));
        assertThrowsStoreIsClosed(query::findLazy);
        assertThrowsStoreIsClosed(query::findLazyCached);
        assertThrowsStoreIsClosed(query::remove);
        assertThrowsStoreIsClosed(() -> query.subscribe().observer(data -> {
        }));

        // describe works, but returns no property info.
        assertEquals("Query for entity <deleted entity type> with 15 conditions", query.describe());
        // describeParameters does not work.
        IllegalStateException exc = assertThrows(IllegalStateException.class, query::describeParameters);
        assertEquals("Query cannot be used after entity type was deleted (e.g. store was closed)", exc.getMessage());

        // setParameter throws.
        assertThrowsEntityDeleted(() -> query.setParameter(simpleString, "value"));
        assertThrowsEntityDeleted(() -> query.setParameters(stringObjectMap, "a", "b"));
        assertThrowsEntityDeleted(() -> query.setParameter(simpleInt, 1));
        assertThrowsEntityDeleted(() -> query.setParameter("oneOf4", new int[]{1, 2}));
        assertThrowsEntityDeleted(() -> query.setParameter("oneOf8", new long[]{1, 2}));
        assertThrowsEntityDeleted(() -> query.setParameters("between", 1, 2));
        assertThrowsEntityDeleted(() -> query.setParameter(simpleInt, 1.0));
        assertThrowsEntityDeleted(() -> query.setParameters("between", 1.0, 2.0));
        assertThrowsEntityDeleted(() -> query.setParameter("oneOfS", new String[]{"a", "b"}));
        assertThrowsEntityDeleted(() -> query.setParameter(simpleByteArray, new byte[]{1, 2}));
    }

    private void assertThrowsStoreIsClosed(ThrowingRunnable runnable) {
        IllegalStateException ex = assertThrows(IllegalStateException.class, runnable);
        assertEquals("Store is closed", ex.getMessage());
    }

    private void assertThrowsEntityDeleted(ThrowingRunnable runnable) {
        IllegalStateException ex = assertThrows(IllegalStateException.class, runnable);
        assertEquals("Query cannot be used after entity type was deleted (e.g. store was closed)", ex.getMessage());
    }

    @Test
    public void testNullNotNull() {
        List<TestEntity> scalars = putTestEntitiesScalars();
        List<TestEntity> strings = putTestEntitiesStrings();
        try (Query<TestEntity> notNull = box.query(simpleString.notNull()).build()) {
            assertEquals(strings.size(), notNull.count());
        }
        try (Query<TestEntity> isNull = box.query(simpleString.isNull()).build()) {
            assertEquals(scalars.size(), isNull.count());
        }
    }

    @Test
    public void testScalarEqual() {
        putTestEntitiesScalars();

        List<TestEntity> all;
        try (Query<TestEntity> query = box.query().equal(simpleInt, 2007).build()) {
            assertEquals(1, query.count());
            assertEquals(8, getFirstNotNull(query).getId());
            assertEquals(8, getUniqueNotNull(query).getId());
            all = query.find();
        }
        assertEquals(1, all.size());
        assertEquals(8, all.get(0).getId());
    }

    @Test
    public void testBooleanEqual() {
        putTestEntitiesScalars();

        try (Query<TestEntity> query = box.query().equal(simpleBoolean, true).build()) {
            assertEquals(5, query.count());
            assertEquals(1, getFirstNotNull(query).getId());
            query.setParameter(simpleBoolean, false);
            assertEquals(5, query.count());
            assertEquals(2, getFirstNotNull(query).getId());
        }

        // Again, but using alias
        try (Query<TestEntity> aliasQuery = box.query().equal(simpleBoolean, true).parameterAlias("bool").build()) {
            assertEquals(5, aliasQuery.count());
            assertEquals(1, getFirstNotNull(aliasQuery).getId());
            aliasQuery.setParameter("bool", false);
            assertEquals(5, aliasQuery.count());
            assertEquals(2, getFirstNotNull(aliasQuery).getId());
        }
    }

    @Test
    public void testNoConditions() {
        List<TestEntity> entities = putTestEntitiesScalars();
        try (Query<TestEntity> query = box.query().build()) {
            List<TestEntity> all = query.find();
            assertEquals(entities.size(), all.size());
            assertEquals(entities.size(), query.count());
        }
    }

    @Test
    public void testScalarNotEqual() {
        List<TestEntity> entities = putTestEntitiesScalars();
        try (Query<TestEntity> query = box.query().notEqual(simpleInt, 2007).notEqual(simpleInt, 2002).build()) {
            assertEquals(entities.size() - 2, query.count());
        }
    }

    @Test
    public void testScalarLessAndGreater() {
        putTestEntitiesScalars();
        try (Query<TestEntity> query = box.query().greater(simpleInt, 2003).less(simpleShort, 2107).build()) {
            assertEquals(3, query.count());
        }
    }

    @Test
    public void integer_lessAndGreater_works() {
        putTestEntitiesScalars();
        int value = 2004;

        buildFindAndAssert(
                box.query().less(TestEntity_.simpleInt, value),
                4,
                (index, item) -> assertTrue(item.getSimpleInt() < value)
        );

        buildFindAndAssert(
                box.query().greater(TestEntity_.simpleInt, value),
                5,
                (index, item) -> assertTrue(item.getSimpleInt() > value)
        );

        buildFindAndAssert(
                box.query().lessOrEqual(TestEntity_.simpleInt, value),
                5,
                (index, item) -> assertTrue(item.getSimpleInt() <= value)
        );

        buildFindAndAssert(
                box.query().greaterOrEqual(TestEntity_.simpleInt, value),
                6,
                (index, item) -> assertTrue(item.getSimpleInt() >= value)
        );
    }

    @Test
    public void testScalarBetween() {
        putTestEntitiesScalars();
        try (Query<TestEntity> query = box.query().between(simpleInt, 2003, 2006).build()) {
            assertEquals(4, query.count());
        }
    }

    @Test
    public void testIntIn() {
        putTestEntitiesScalars();

        int[] valuesInt = {1, 1, 2, 3, 2003, 2007, 2002, -1};
        try (Query<TestEntity> query = box.query().in(simpleInt, valuesInt).parameterAlias("int").build()) {
            assertEquals(3, query.count());

            int[] valuesInt2 = {2003};
            query.setParameter(simpleInt, valuesInt2);
            assertEquals(1, query.count());

            int[] valuesInt3 = {2003, 2007};
            query.setParameter("int", valuesInt3);
            assertEquals(2, query.count());
        }
    }

    @Test
    public void testLongIn() {
        putTestEntitiesScalars();

        long[] valuesLong = {1, 1, 2, 3, 3003, 3007, 3002, -1};
        try (Query<TestEntity> query = box.query().in(simpleLong, valuesLong).parameterAlias("long").build()) {
            assertEquals(3, query.count());

            long[] valuesLong2 = {3003};
            query.setParameter(simpleLong, valuesLong2);
            assertEquals(1, query.count());

            long[] valuesLong3 = {3003, 3007};
            query.setParameter("long", valuesLong3);
            assertEquals(2, query.count());
        }
    }

    @Test
    public void testIntNotIn() {
        putTestEntitiesScalars();

        int[] valuesInt = {1, 1, 2, 3, 2003, 2007, 2002, -1};
        try (Query<TestEntity> query = box.query().notIn(simpleInt, valuesInt).parameterAlias("int").build()) {
            assertEquals(7, query.count());

            int[] valuesInt2 = {2003};
            query.setParameter(simpleInt, valuesInt2);
            assertEquals(9, query.count());

            int[] valuesInt3 = {2003, 2007};
            query.setParameter("int", valuesInt3);
            assertEquals(8, query.count());
        }
    }

    @Test
    public void testLongNotIn() {
        putTestEntitiesScalars();

        long[] valuesLong = {1, 1, 2, 3, 3003, 3007, 3002, -1};
        try (Query<TestEntity> query = box.query().notIn(simpleLong, valuesLong).parameterAlias("long").build()) {
            assertEquals(7, query.count());

            long[] valuesLong2 = {3003};
            query.setParameter(simpleLong, valuesLong2);
            assertEquals(9, query.count());

            long[] valuesLong3 = {3003, 3007};
            query.setParameter("long", valuesLong3);
            assertEquals(8, query.count());
        }
    }

    @Test
    public void offset_limit_find() {
        putTestEntitiesScalars();
        try (Query<TestEntity> query = box.query().greater(simpleInt, 2002).less(simpleShort, 2108).build()) {
            assertEquals(5, query.count());

            assertEquals(4, query.find(1, 0).size());
            assertEquals(1, query.find(4, 0).size());
            assertEquals(2, query.find(0, 2).size());
            List<TestEntity> list = query.find(1, 2);
            assertEquals(2, list.size());
            assertEquals(2004, list.get(0).getSimpleInt());
            assertEquals(2005, list.get(1).getSimpleInt());

            OffsetLimitFunction find = (offset, limit) -> query.find(offset, limit).size();
            assertOffsetLimitEdgeCases(find);
        }
    }

    @Test
    public void offset_limit_findIds() {
        putTestEntitiesScalars();
        try (Query<TestEntity> query = box.query().greater(simpleInt, 2002).less(simpleShort, 2108).build()) {
            assertEquals(5, query.count());

            assertEquals(4, query.findIds(1, 0).length);
            assertEquals(1, query.findIds(4, 0).length);
            assertEquals(2, query.findIds(0, 2).length);
            long[] list = query.findIds(1, 2);
            assertEquals(2, list.length);
            assertEquals(5, list[0]);
            assertEquals(6, list[1]);

            OffsetLimitFunction findIds = (offset, limit) -> query.findIds(offset, limit).length;
            assertOffsetLimitEdgeCases(findIds);
        }
    }

    private interface OffsetLimitFunction {
        int applyAndCount(long offset, long limit);
    }

    private void assertOffsetLimitEdgeCases(OffsetLimitFunction function) {
        // Max value
        if (TestUtils.is32BitJVM()) {
            // When running 32-bit ObjectBox limit and offset max is limited to 32-bit unsigned integer.
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> function.applyAndCount(Long.MAX_VALUE, Long.MAX_VALUE));
            assertEquals("Invalid offset (9223372036854775807): exceeds the maximum value allowed on this platform (4294967295)",
                    ex.getMessage());
            // Ensure max allowed value works.
            // Note: currently offset + limit must not exceed 32-bit unsigned integer max.
            assertEquals(0, function.applyAndCount(Integer.MAX_VALUE * 2L + 1, 0));
            assertEquals(5, function.applyAndCount(0, Integer.MAX_VALUE * 2L + 1));
        } else {
            // 64-bit JVM
            assertEquals(0, function.applyAndCount(Long.MAX_VALUE, Long.MAX_VALUE));
        }

        // Min value
        IllegalArgumentException exOffset = assertThrows(IllegalArgumentException.class,
                () -> function.applyAndCount(Long.MIN_VALUE, 0));
        assertEquals("Invalid offset (-9223372036854775808): must be zero or positive", exOffset.getMessage());
        IllegalArgumentException exLimit = assertThrows(IllegalArgumentException.class,
                () -> function.applyAndCount(0, Long.MIN_VALUE));
        assertEquals("Invalid limit (-9223372036854775808): must be zero or positive", exLimit.getMessage());
    }

    @Test
    public void testString() {
        List<TestEntity> entities = putTestEntitiesStrings();
        int count = entities.size();
        
        try (Query<TestEntity> equal = box.query()
                .equal(simpleString, "banana", StringOrder.CASE_INSENSITIVE)
                .build()) {
            assertEquals(1, getUniqueNotNull(equal).getId());
        }
        try (Query<TestEntity> notEqual = box.query()
                .notEqual(simpleString, "banana", StringOrder.CASE_INSENSITIVE)
                .build()) {
            assertEquals(count - 1, notEqual.count());
        }
        try (Query<TestEntity> startsEndsWith = box.query()
                .startsWith(simpleString, "ba", StringOrder.CASE_INSENSITIVE)
                .endsWith(simpleString, "shake", StringOrder.CASE_INSENSITIVE)
                .build()) {
            assertEquals(4, getUniqueNotNull(startsEndsWith).getId());
        }

        // contains
        try (Query<TestEntity> contains = box.query()
                .contains(simpleString, "nana", StringOrder.CASE_INSENSITIVE)
                .build()) {
            assertEquals(2, contains.count());
        }
        // Verify case-sensitive setting has no side effects for non-ASCII characters
        box.put(createTestEntity("Note that Îñţérñåţîöñåļîžåţîờñ is key", 6));
        try (Query<TestEntity> contains = box.query()
                .contains(simpleString, "Îñţérñåţîöñåļîžåţîờñ", StringOrder.CASE_SENSITIVE)
                .build()) {
            assertEquals(1, contains.count());
        }
        try (Query<TestEntity> contains = box.query()
                .contains(simpleString, "Îñţérñåţîöñåļîžåţîờñ", StringOrder.CASE_INSENSITIVE)
                .build()) {
            assertEquals(1, contains.count());
        }
    }

    @Test
    public void testStringArray() {
        putTestEntitiesStrings();

        // Using contains should not work on String array.
        Exception exception = assertThrows(UnsupportedOperationException.class,
                () -> box.query().contains(simpleStringArray, "banana", StringOrder.CASE_INSENSITIVE));
        assertEquals("For String[] only containsElement() is supported at this time.", exception.getMessage());

        // containsElement(prop, value) matches if value is equal to one of the array items.
        // Verify by not matching entity where 'banana' is only a substring of an array item ('banana milk shake').
        List<TestEntity> results;
        try (Query<TestEntity> containsElement = box.query()
                .containsElement(simpleStringArray, "banana", StringOrder.CASE_INSENSITIVE)
                .build()) {
            results = containsElement.find();
        }
        assertEquals(1, results.size());
        assertEquals("banana", results.get(0).getSimpleStringArray()[0]);
    }

    @Test
    public void testStringLess() {
        putTestEntitiesStrings();
        putTestEntity("BaNaNa Split", 100);
        List<TestEntity> entities;
        try (Query<TestEntity> query = box.query()
                .less(simpleString, "banana juice", StringOrder.CASE_INSENSITIVE)
                .order(simpleString)
                .build()) {
            entities = query.find();
            assertEquals(2, entities.size());
            assertEquals("apple", entities.get(0).getSimpleString());
            assertEquals("banana", entities.get(1).getSimpleString());

            query.setParameter(simpleString, "BANANA MZ");
            entities = query.find();
        }
        assertEquals(3, entities.size());
        assertEquals("apple", entities.get(0).getSimpleString());
        assertEquals("banana", entities.get(1).getSimpleString());
        assertEquals("banana milk shake", entities.get(2).getSimpleString());

        // Case sensitive
        try (Query<TestEntity> queryCaseSens = box.query()
                .less(simpleString, "BANANA", StringOrder.CASE_SENSITIVE)
                .order(simpleString)
                .build()) {
            assertEquals(0, queryCaseSens.count());

            queryCaseSens.setParameter(simpleString, "banana a");
            entities = queryCaseSens.find();
        }
        assertEquals(3, entities.size());
        assertEquals("apple", entities.get(0).getSimpleString());
        assertEquals("banana", entities.get(1).getSimpleString());
        assertEquals("BaNaNa Split", entities.get(2).getSimpleString());
    }

    @Test
    public void string_lessOrEqual_works() {
        putTestEntitiesStrings();

        ListItemAsserter<TestEntity> lessOrEqualAsserter = (index, item) -> {
            if (index == 0) assertEquals("apple", item.getSimpleString());
            if (index == 1) assertEquals("banana", item.getSimpleString());
            if (index == 2) assertEquals("banana milk shake", item.getSimpleString());
        };

        buildFindAndAssert(
                box.query()
                        .lessOrEqual(TestEntity_.simpleString, "BANANA MILK SHAKE", StringOrder.CASE_INSENSITIVE)
                        .order(TestEntity_.simpleString),
                3,
                lessOrEqualAsserter
        );

        buildFindAndAssert(
                box.query()
                        .lessOrEqual(TestEntity_.simpleString, "banana milk shake", StringOrder.CASE_SENSITIVE)
                        .order(TestEntity_.simpleString),
                3,
                lessOrEqualAsserter
        );
    }

    @Test
    public void testStringGreater() {
        putTestEntitiesStrings();
        putTestEntity("FOO", 100);
        List<TestEntity> entities;
        try (Query<TestEntity> query = box.query()
                .greater(simpleString, "banana juice", StringOrder.CASE_INSENSITIVE)
                .order(simpleString)
                .build()) {
            entities = query.find();
            assertEquals(4, entities.size());
            assertEquals("banana milk shake", entities.get(0).getSimpleString());
            assertEquals("bar", entities.get(1).getSimpleString());
            assertEquals("FOO", entities.get(2).getSimpleString());
            assertEquals("foo bar", entities.get(3).getSimpleString());

            query.setParameter(simpleString, "FO");
            entities = query.find();
        }
        assertEquals(2, entities.size());
        assertEquals("FOO", entities.get(0).getSimpleString());
        assertEquals("foo bar", entities.get(1).getSimpleString());

        // Case sensitive
        try (Query<TestEntity> queryCaseSens = box.query()
                .greater(simpleString, "banana", StringOrder.CASE_SENSITIVE)
                .order(simpleString)
                .build()) {
            entities = queryCaseSens.find();
        }
        assertEquals(3, entities.size());
        assertEquals("banana milk shake", entities.get(0).getSimpleString());
        assertEquals("bar", entities.get(1).getSimpleString());
        assertEquals("foo bar", entities.get(2).getSimpleString());
    }

    @Test
    public void string_greaterOrEqual_works() {
        putTestEntitiesStrings();

        ListItemAsserter<TestEntity> greaterOrEqualAsserter = (index, item) -> {
            if (index == 0) assertEquals("banana milk shake", item.getSimpleString());
            if (index == 1) assertEquals("bar", item.getSimpleString());
            if (index == 2) assertEquals("foo bar", item.getSimpleString());
        };

        buildFindAndAssert(
                box.query()
                        .greaterOrEqual(TestEntity_.simpleString, "BANANA MILK SHAKE", StringOrder.CASE_INSENSITIVE)
                        .order(TestEntity_.simpleString),
                3,
                greaterOrEqualAsserter
        );

        buildFindAndAssert(
                box.query()
                        .greaterOrEqual(TestEntity_.simpleString, "banana milk shake", StringOrder.CASE_SENSITIVE)
                        .order(TestEntity_.simpleString),
                3,
                greaterOrEqualAsserter
        );
    }

    @Test
    public void testStringIn() {
        putTestEntitiesStrings();
        putTestEntity("BAR", 100);
        String[] values = {"bar", "foo bar"};
        List<TestEntity> entities;
        try (Query<TestEntity> query = box.query()
                .in(simpleString, values, StringOrder.CASE_INSENSITIVE)
                .order(simpleString, OrderFlags.CASE_SENSITIVE)
                .build()) {
            entities = query.find();
            assertEquals(3, entities.size());
            assertEquals("BAR", entities.get(0).getSimpleString());
            assertEquals("bar", entities.get(1).getSimpleString());
            assertEquals("foo bar", entities.get(2).getSimpleString());

            String[] values2 = {"bar"};
            query.setParameter(simpleString, values2);
            entities = query.find();
        }
        assertEquals(2, entities.size());
        assertEquals("BAR", entities.get(0).getSimpleString());
        assertEquals("bar", entities.get(1).getSimpleString());

        // Case sensitive
        try (Query<TestEntity> queryCaseSens = box.query()
                .in(simpleString, values, StringOrder.CASE_SENSITIVE)
                .order(simpleString)
                .build()) {
            entities = queryCaseSens.find();
        }
        assertEquals(2, entities.size());
        assertEquals("bar", entities.get(0).getSimpleString());
        assertEquals("foo bar", entities.get(1).getSimpleString());
    }

    @Test
    public void testByteArrayEqualsAndSetParameter() {
        putTestEntitiesScalars();

        byte[] value = {1, 2, (byte) 2000};
        try (Query<TestEntity> query = box.query()
                .equal(simpleByteArray, value)
                .parameterAlias("bytes")
                .build()) {
            assertEquals(1, query.count());
            TestEntity first = query.findFirst();
            assertNotNull(first);
            assertArrayEquals(value, first.getSimpleByteArray());

            byte[] value2 = {1, 2, (byte) 2001};
            query.setParameter(simpleByteArray, value2);

            assertEquals(1, query.count());
            TestEntity first2 = query.findFirst();
            assertNotNull(first2);
            assertArrayEquals(value2, first2.getSimpleByteArray());

            byte[] value3 = {1, 2, (byte) 2002};
            query.setParameter("bytes", value3);

            assertEquals(1, query.count());
            TestEntity first3 = query.findFirst();
            assertNotNull(first3);
            assertArrayEquals(value3, first3.getSimpleByteArray());
        }
    }

    @Test
    public void byteArray_lessAndGreater_works() {
        putTestEntitiesScalars();
        byte[] value = {1, 2, (byte) 2005};

        // Java does not have compareTo for arrays, so just make sure it's not equal to the value.
        ListItemAsserter<TestEntity> resultsNotEqual = (index, item) -> assertFalse(Arrays.equals(value, item.getSimpleByteArray()));

        buildFindAndAssert(
                box.query().less(TestEntity_.simpleByteArray, value), 5, resultsNotEqual
        );

        buildFindAndAssert(
                box.query().greater(TestEntity_.simpleByteArray, value), 4, resultsNotEqual
        );

        buildFindAndAssert(
                box.query().lessOrEqual(TestEntity_.simpleByteArray, value),
                6,
                (index, item) -> {
                    if (index == 5) {
                        assertArrayEquals(value, item.getSimpleByteArray());
                    } else {
                        assertFalse(Arrays.equals(value, item.getSimpleByteArray()));
                    }
                }
        );

        buildFindAndAssert(
                box.query().greaterOrEqual(TestEntity_.simpleByteArray, value),
                5,
                (index, item) -> {
                    if (index == 0) {
                        assertArrayEquals(value, item.getSimpleByteArray());
                    } else {
                        assertFalse(Arrays.equals(value, item.getSimpleByteArray()));
                    }
                }
        );

        // greater and less
        byte[] valueGreater = {1, 2, (byte) 2002};
        buildFindAndAssert(
                box.query().greater(TestEntity_.simpleByteArray, valueGreater).less(TestEntity_.simpleByteArray, value),
                2,
                (index, item) -> {
                    assertFalse(Arrays.equals(value, item.getSimpleByteArray()));
                    assertFalse(Arrays.equals(valueGreater, item.getSimpleByteArray()));
                }
        );
    }

    @Test
    public void float_lessAndGreater_works() {
        putTestEntitiesScalars();
        float value = 400.5f;

        buildFindAndAssert(
                box.query().less(TestEntity_.simpleFloat, value),
                5,
                (index, item) -> assertTrue(item.getSimpleFloat() < value)
        );

        buildFindAndAssert(
                box.query().lessOrEqual(TestEntity_.simpleFloat, value),
                6,
                (index, item) -> assertTrue(item.getSimpleFloat() <= value)
        );

        buildFindAndAssert(
                box.query().greater(TestEntity_.simpleFloat, value),
                4,
                (index, item) -> assertTrue(item.getSimpleFloat() > value)
        );

        buildFindAndAssert(
                box.query().greaterOrEqual(TestEntity_.simpleFloat, value),
                5,
                (index, item) -> assertTrue(item.getSimpleFloat() >= value)
        );

        float valueLess = 400.51f;
        float valueGreater = 400.29f;
        buildFindAndAssert(
                box.query().greater(TestEntity_.simpleFloat, valueGreater).less(TestEntity_.simpleFloat, valueLess),
                3,
                (index, item) -> {
                    assertTrue(item.getSimpleFloat() < valueLess);
                    assertTrue(item.getSimpleFloat() > valueGreater);
                }
        );
    }

    @Test
    public void double_lessAndGreater_works() {
        putTestEntitiesScalars();
        // Note: calculation matches putTestEntitiesScalars.
        double value = 2000 + 2005 / 100f;

        buildFindAndAssert(
                box.query().less(TestEntity_.simpleDouble, value),
                5,
                (index, item) -> assertTrue(item.getSimpleDouble() < value)
        );

        buildFindAndAssert(
                box.query().lessOrEqual(TestEntity_.simpleDouble, value),
                6,
                (index, item) -> assertTrue(item.getSimpleDouble() <= value)
        );

        buildFindAndAssert(
                box.query().greater(TestEntity_.simpleDouble, value),
                4,
                (index, item) -> assertTrue(item.getSimpleDouble() > value)
        );

        buildFindAndAssert(
                box.query().greaterOrEqual(TestEntity_.simpleDouble, value),
                5,
                (index, item) -> assertTrue(item.getSimpleDouble() >= value)
        );

        double valueLess = 2020.051;
        double valueGreater = 2020.029;
        buildFindAndAssert(
                box.query().greater(TestEntity_.simpleDouble, valueGreater).less(TestEntity_.simpleDouble, valueLess),
                3,
                (index, item) -> {
                    assertTrue(item.getSimpleDouble() < valueLess);
                    assertTrue(item.getSimpleDouble() > valueGreater);
                }
        );
    }

    @Test
    // Android JNI seems to have a limit of 512 local jobject references. Internally, we must delete those temporary
    // references when processing lists. This is the test for that.
    public void testBigResultList() {
        List<TestEntity> entities = new ArrayList<>();
        String sameValueForAll = "schrodinger";
        for (int i = 0; i < 10000; i++) {
            TestEntity entity = createTestEntity(sameValueForAll, i);
            entities.add(entity);
        }
        box.put(entities);
        int count = entities.size();
        try (Query<TestEntity> query = box.query()
                .equal(simpleString, sameValueForAll, StringOrder.CASE_INSENSITIVE)
                .build()) {
            List<TestEntity> entitiesQueried = query.find();
            assertEquals(count, entitiesQueried.size());
        }
    }

    @Test
    public void testEqualStringOrder() {
        putTestEntitiesStrings();
        putTestEntity("BAR", 100);
        try (Query<TestEntity> queryInSens = box.query()
                .equal(simpleString, "bar", StringOrder.CASE_INSENSITIVE)
                .build()) {
            assertEquals(2, queryInSens.count());
        }
        try (Query<TestEntity> querySens = box.query()
                .equal(simpleString, "bar", StringOrder.CASE_SENSITIVE)
                .build()) {
            assertEquals(1, querySens.count());
        }
    }

    @Test
    public void testOrder() {
        putTestEntitiesStrings();
        putTestEntity("BAR", 100);
        try (Query<TestEntity> query = box.query().order(simpleString).build()) {
            List<TestEntity> result = query.find();
            assertEquals(6, result.size());
            assertEquals("apple", result.get(0).getSimpleString());
            assertEquals("banana", result.get(1).getSimpleString());
            assertEquals("banana milk shake", result.get(2).getSimpleString());
            assertEquals("bar", result.get(3).getSimpleString());
            assertEquals("BAR", result.get(4).getSimpleString());
            assertEquals("foo bar", result.get(5).getSimpleString());
        }
    }

    @Test
    public void testOrderDescCaseNullLast() {
        putTestEntity(null, 1000);
        putTestEntity("BAR", 100);
        putTestEntitiesStrings();
        int flags = QueryBuilder.CASE_SENSITIVE | QueryBuilder.NULLS_LAST | QueryBuilder.DESCENDING;
        try (Query<TestEntity> query = box.query().order(simpleString, flags).build()) {
            List<TestEntity> result = query.find();
            assertEquals(7, result.size());
            assertEquals("foo bar", result.get(0).getSimpleString());
            assertEquals("bar", result.get(1).getSimpleString());
            assertEquals("banana milk shake", result.get(2).getSimpleString());
            assertEquals("banana", result.get(3).getSimpleString());
            assertEquals("apple", result.get(4).getSimpleString());
            assertEquals("BAR", result.get(5).getSimpleString());
            assertNull(result.get(6).getSimpleString());
        }
    }

    @Test
    public void testRemove() {
        putTestEntitiesScalars();
        try (Query<TestEntity> query = box.query().greater(simpleInt, 2003).build()) {
            assertEquals(6, query.remove());
        }
        assertEquals(4, box.count());
    }

    @Test
    public void findFirstId() {
        putTestEntitiesScalars();
        try (Query<TestEntity> query = box.query(simpleInt.greater(2006)).build()) {
            assertEquals(8, query.findFirstId());
        }
        // No result.
        try (Query<TestEntity> query = box.query(simpleInt.equal(-1)).build()) {
            assertEquals(0, query.findFirstId());
        }
    }

    @Test
    public void findUniqueId() {
        putTestEntitiesScalars();
        try (Query<TestEntity> query = box.query(simpleInt.equal(2006)).build()) {
            assertEquals(7, query.findUniqueId());
        }
        // No result.
        try (Query<TestEntity> query = box.query(simpleInt.equal(-1)).build()) {
            assertEquals(0, query.findUniqueId());
        }
        // More than one result.
        try (Query<TestEntity> query = box.query(simpleInt.greater(2006)).build()) {
            NonUniqueResultException e = assertThrows(NonUniqueResultException.class, query::findUniqueId);
            assertEquals("Query does not have a unique result (more than one result): 3", e.getMessage());
        }
    }

    @Test
    public void testFindIds() {
        putTestEntitiesScalars();
        try (Query<TestEntity> queryAll = box.query().build()) {
            assertEquals(10, queryAll.findIds().length);
        }

        try (Query<TestEntity> query = box.query().greater(simpleInt, 2006).build()) {
            long[] keys = query.findIds();
            assertEquals(3, keys.length);
            assertEquals(8, keys[0]);
            assertEquals(9, keys[1]);
            assertEquals(10, keys[2]);
        }
    }

    @Test
    public void testFindIdsWithOrder() {
        putTestEntitiesScalars();
        long[] ids;
        try (Query<TestEntity> query = box.query().orderDesc(TestEntity_.simpleInt).build()) {
            ids = query.findIds();
            assertEquals(10, ids.length);
            assertEquals(10, ids[0]);
            assertEquals(1, ids[9]);

            ids = query.findIds(3, 2);
        }
        assertEquals(2, ids.length);
        assertEquals(7, ids[0]);
        assertEquals(6, ids[1]);
    }

    @Test
    public void testOr() {
        putTestEntitiesScalars();
        List<TestEntity> entities;
        try (Query<TestEntity> query = box.query()
                .equal(simpleInt, 2007)
                .or().equal(simpleLong, 3002)
                .build()) {
            entities = query.find();
        }
        assertEquals(2, entities.size());
        assertEquals(3002, entities.get(0).getSimpleLong());
        assertEquals(2007, entities.get(1).getSimpleInt());
    }

    @Test
    public void testOr_bad1() {
        assertNoPreviousCondition(() -> box.query().or());
    }

    private void assertNoPreviousCondition(ThrowingRunnable runnable) {
        IllegalStateException ex = assertThrows(IllegalStateException.class, runnable);
        assertEquals("No previous condition. Use operators like and() and or() only between two conditions.",
                ex.getMessage());
    }

    @SuppressWarnings("resource") // Throws RuntimeException, so not closing Builder/Query is fine.
    @Test
    public void testOr_bad2() {
        assertIncompleteLogicCondition(() -> box.query().equal(simpleInt, 1).or().build());
    }

    private void assertIncompleteLogicCondition(ThrowingRunnable runnable) {
        IllegalStateException ex = assertThrows(IllegalStateException.class, runnable);
        assertEquals("Incomplete logic condition. Use or()/and() between two conditions only.",
                ex.getMessage());
    }

    @Test
    public void testAnd() {
        putTestEntitiesScalars();
        // Result if OR precedence (wrong): {}, AND precedence (expected): {2008}
        List<TestEntity> entities;
        try (Query<TestEntity> query = box.query()
                .equal(simpleInt, 2006)
                .and().equal(simpleInt, 2007)
                .or().equal(simpleInt, 2008)
                .build()) {
            entities = query.find();
        }
        assertEquals(1, entities.size());
        assertEquals(2008, entities.get(0).getSimpleInt());
    }

    @Test
    public void testAnd_bad1() {
        assertNoPreviousCondition(() -> box.query().and());
    }

    @SuppressWarnings("resource") // Throws RuntimeException, so not closing Builder/Query is fine.
    @Test
    public void testAnd_bad2() {
        assertIncompleteLogicCondition(() -> box.query().equal(simpleInt, 1).and().build());
    }

    @SuppressWarnings("resource") // Throws RuntimeException, so not closing Builder/Query is fine.
    @Test
    public void testOrAfterAnd() {
        assertOperatorIsPending(() -> box.query()
                .equal(simpleInt, 1)
                .and()
                .or()
                .equal(simpleInt, 2)
                .build());
    }

    @SuppressWarnings("resource") // Throws RuntimeException, so not closing Builder/Query is fine.
    @Test
    public void testOrderAfterAnd() {
        assertOperatorIsPending(() -> box.query()
                .equal(simpleInt, 1)
                .and().order(simpleInt)
                .equal(simpleInt, 2)
                .build());
    }

    private void assertOperatorIsPending(ThrowingRunnable runnable) {
        IllegalStateException ex = assertThrows(IllegalStateException.class, runnable);
        assertEquals("Another operator is pending. Use operators like and() and or() only between two conditions.",
                ex.getMessage());
    }

    @Test
    public void testSetParameterInt() {
        String versionNative = BoxStore.getVersionNative();
        String minVersion = "1.5.1-2018-06-21";
        String versionStart = versionNative.substring(0, minVersion.length());
        assertTrue(versionStart, versionStart.compareTo(minVersion) >= 0);

        putTestEntitiesScalars();
        try (Query<TestEntity> query = box.query()
                .equal(simpleInt, 2007)
                .parameterAlias("foo")
                .build()) {
            assertEquals(8, getUniqueNotNull(query).getId());
            query.setParameter(simpleInt, 2004);
            assertEquals(5, getUniqueNotNull(query).getId());

            query.setParameter("foo", 2002);
            assertEquals(3, getUniqueNotNull(query).getId());
        }
    }

    @Test
    public void testSetParameter2Ints() {
        putTestEntitiesScalars();
        try (Query<TestEntity> query = box.query()
                .between(simpleInt, 2005, 2008)
                .parameterAlias("foo")
                .build()) {
            assertEquals(4, query.count());
            query.setParameters(simpleInt, 2002, 2003);
            List<TestEntity> entities = query.find();
            assertEquals(2, entities.size());
            assertEquals(3, entities.get(0).getId());
            assertEquals(4, entities.get(1).getId());

            query.setParameters("foo", 2007, 2007);
            assertEquals(8, getUniqueNotNull(query).getId());
        }
    }

    @Test
    public void testSetParameterFloat() {
        putTestEntitiesScalars();
        try (Query<TestEntity> query = box.query()
                .greater(simpleFloat, 400.65)
                .parameterAlias("foo")
                .build()) {
            assertEquals(3, query.count());
            query.setParameter(simpleFloat, 400.75);
            assertEquals(2, query.count());

            query.setParameter("foo", 400.85);
            assertEquals(1, query.count());
        }
    }

    @Test
    public void testSetParameter2Floats() {
        putTestEntitiesScalars();
        try (Query<TestEntity> query = box.query()
                .between(simpleFloat, 400.15, 400.75)
                .parameterAlias("foo")
                .build()) {
            assertEquals(6, query.count());
            query.setParameters(simpleFloat, 400.65, 400.85);
            List<TestEntity> entities = query.find();
            assertEquals(2, entities.size());
            assertEquals(8, entities.get(0).getId());
            assertEquals(9, entities.get(1).getId());

            query.setParameters("foo", 400.45, 400.55);
            assertEquals(6, getUniqueNotNull(query).getId());
        }
    }

    @Test
    public void testSetParameterString() {
        putTestEntitiesStrings();
        try (Query<TestEntity> query = box.query()
                .equal(simpleString, "banana", StringOrder.CASE_INSENSITIVE)
                .parameterAlias("foo")
                .build()) {
            assertEquals(1, getUniqueNotNull(query).getId());
            query.setParameter(simpleString, "bar");
            assertEquals(3, getUniqueNotNull(query).getId());

            assertNull(query.setParameter(simpleString, "not here!").findUnique());

            query.setParameter("foo", "apple");
            assertEquals(2, getUniqueNotNull(query).getId());
        }
    }

    /**
     * <a href="https://github.com/objectbox/objectbox-java/issues/834">Using alias on condition combined with AND or OR fails #834</a>
     */
    @Test
    public void parameterAlias_combinedConditions() {
        putTestEntitiesScalars();

        try (Query<TestEntity> query = box.query()
                .greater(simpleInt, 0).parameterAlias("greater")
                .or()
                .less(simpleInt, 0).parameterAlias("less")
                .build()) {
            List<TestEntity> results = query
                    .setParameter("greater", 2008)
                    .setParameter("less", 2001)
                    .find();
            assertEquals(2, results.size());
            assertEquals(2000, results.get(0).getSimpleInt());
            assertEquals(2009, results.get(1).getSimpleInt());
        }
    }

    @Test
    public void testForEach() {
        List<TestEntity> testEntities = putTestEntitiesStrings();
        final StringBuilder stringBuilder = new StringBuilder();
        try (Query<TestEntity> query = box.query()
                .startsWith(simpleString, "banana", StringOrder.CASE_INSENSITIVE)
                .build()) {
            query.forEach(data -> stringBuilder.append(data.getSimpleString()).append('#'));
        }
        assertEquals("banana#banana milk shake#", stringBuilder.toString());

        // Verify that box does not hang on to the read-only TX by doing a put
        box.put(new TestEntity());
        assertEquals(testEntities.size() + 1, box.count());
    }

    @Test
    public void testForEachBreak() {
        putTestEntitiesStrings();
        final StringBuilder stringBuilder = new StringBuilder();
        try (Query<TestEntity> query = box.query()
                .startsWith(simpleString, "banana", StringOrder.CASE_INSENSITIVE)
                .build()) {
            query.forEach(data -> {
                stringBuilder.append(data.getSimpleString());
                throw new BreakForEach();
            });
        }
        assertEquals("banana", stringBuilder.toString());
    }

    @Test
    // TODO can we improve? More than just "still works"?
    public void testQueryAttempts() {
        store.close();
        BoxStoreBuilder builder = createBuilderWithTestModel().directory(boxStoreDir)
                .queryAttempts(5)
                .failedReadTxAttemptCallback((result, error) -> {
                    if (error != null) {
                        error.printStackTrace();
                    }
                });
        builder.entity(new TestEntity_());

        store = builder.build();
        putTestEntitiesScalars();

        Query<TestEntity> query = store.boxFor(TestEntity.class).query().equal(simpleInt, 2007).build();
        assertEquals(2007, getFirstNotNull(query).getSimpleInt());
    }

    @Test
    public void date_equal_and_setParameter_works() {
        Date now = new Date();
        TestEntity entity = new TestEntity();
        entity.setDate(now);
        Box<TestEntity> box = store.boxFor(TestEntity.class);
        box.put(entity);

        try (Query<TestEntity> query = box.query(TestEntity_.date.equal(0)).build()) {
            assertEquals(0, query.count());
            query.setParameter(TestEntity_.date, now);
            assertEquals(1, query.count());
        }

        // Again, but using alias
        try (Query<TestEntity> aliasQuery = box.query(TestEntity_.date.equal(0)).parameterAlias("date").build()) {
            assertEquals(0, aliasQuery.count());
            aliasQuery.setParameter("date", now);
            assertEquals(1, aliasQuery.count());
        }
    }

    @Test
    public void date_between_works() {
        putTestEntitiesScalars();
        try (Query<TestEntity> query = box.query(date.between(new Date(3002L), new Date(3008L))).build()) {
            assertEquals(7, query.count());
        }
    }

    @Test
    public void date_lessAndGreater_works() {
        putTestEntitiesScalars();
        try (Query<TestEntity> query = box.query(date.less(new Date(3002L))).build()) {
            assertEquals(2, query.count());
        }
        try (Query<TestEntity> query = box.query(date.lessOrEqual(new Date(3003L))).build()) {
            assertEquals(4, query.count());
        }
        try (Query<TestEntity> query = box.query(date.greater(new Date(3008L))).build()) {
            assertEquals(1, query.count());
        }
        try (Query<TestEntity> query = box.query(date.greaterOrEqual(new Date(3008L))).build()) {
            assertEquals(2, query.count());
        }
    }

    @Test
    public void date_oneOf_works() {
        putTestEntitiesScalars();
        Date[] valuesDate = new Date[]{new Date(3002L), new Date(), new Date(0)};
        try (Query<TestEntity> query = box.query(date.oneOf(valuesDate)).build()) {
            assertEquals(1, query.count());
        }
        Date[] valuesDate2 = new Date[]{new Date()};
        try (Query<TestEntity> query = box.query(date.oneOf(valuesDate2)).build()) {
            assertEquals(0, query.count());
        }
        Date[] valuesDate3 = new Date[]{new Date(3002L), new Date(3009L)};
        try (Query<TestEntity> query = box.query(date.oneOf(valuesDate3)).build()) {
            assertEquals(2, query.count());
        }
    }

    @Test
    public void date_notOneOf_works() {
        putTestEntitiesScalars();
        Date[] valuesDate = new Date[]{new Date(3002L), new Date(), new Date(0)};
        try (Query<TestEntity> query = box.query(date.notOneOf(valuesDate)).build()) {
            assertEquals(9, query.count());
        }
        Date[] valuesDate2 = new Date[]{new Date()};
        try (Query<TestEntity> query = box.query(date.notOneOf(valuesDate2)).build()) {
            assertEquals(10, query.count());
        }
        Date[] valuesDate3 = new Date[]{new Date(3002L), new Date(3009L)};
        try (Query<TestEntity> query = box.query(date.notOneOf(valuesDate3)).build()) {
            assertEquals(8, query.count());
        }
    }

    @Test
    public void testFailedUnique_exceptionListener() {
        final Exception[] exs = {null};
        DbExceptionListener exceptionListener = e -> exs[0] = e;
        putTestEntitiesStrings();
        try (Query<TestEntity> query = box.query().build()) {
            store.setDbExceptionListener(exceptionListener);
            try {
                query.findUnique();
                fail("Should have thrown");
            } catch (NonUniqueResultException e) {
                assertSame(e, exs[0]);
            }
        }
    }

    @Test
    public void testFailedUnique_cancelException() {
        final Exception[] exs = {null};
        DbExceptionListener exceptionListener = e -> {
            if (exs[0] != null) throw new RuntimeException("Called more than once");
            exs[0] = e;
            DbExceptionListener.cancelCurrentException();
        };
        putTestEntitiesStrings();
        try (Query<TestEntity> query = box.query().build()) {
            store.setDbExceptionListener(exceptionListener);
            TestEntity object = query.findUnique();
            assertNull(object);
        }
        assertNotNull(exs[0]);
        assertEquals(exs[0].getClass(), NonUniqueResultException.class);
    }

    @Test
    public void testDescribe() {
        // Note: description string correctness is fully asserted in core library.

        // No conditions.
        try (Query<TestEntity> queryNoConditions = box.query().build()) {
            assertEquals("Query for entity TestEntity with 1 conditions", queryNoConditions.describe());
            assertEquals("TRUE", queryNoConditions.describeParameters());
        }

        // Some conditions.
        try (Query<TestEntity> query = box.query()
                .equal(TestEntity_.simpleString, "Hello", StringOrder.CASE_INSENSITIVE)
                .or().greater(TestEntity_.simpleInt, 42)
                .build()) {
            String describeActual = query.describe();
            assertTrue(describeActual.startsWith("Query for entity TestEntity with 3 conditions with properties "));
            // Note: the order properties are listed in is not fixed.
            assertTrue(describeActual.contains(TestEntity_.simpleString.name));
            assertTrue(describeActual.contains(TestEntity_.simpleInt.name));
            assertEquals("(simpleString ==(i) \"Hello\"\n OR simpleInt > 42)", query.describeParameters());
        }
    }

    private <T> void buildFindAndAssert(QueryBuilder<T> builder, int expectedCount, ListItemAsserter<T> asserter) {
        try (Query<T> query = builder.build()) {
            List<T> results = query.find();
            assertEquals(expectedCount, results.size());
            for (int i = 0; i < results.size(); i++) {
                asserter.assertListItem(i, results.get(i));
            }
        }
    }

    private interface ListItemAsserter<T> {
        void assertListItem(int index, T item);
    }

    private <T> T getFirstNotNull(Query<T> query) {
        T first = query.findFirst();
        assertNotNull(first);
        return first;
    }

    private <T> T getUniqueNotNull(Query<T> query) {
        T first = query.findUnique();
        assertNotNull(first);
        return first;
    }
}
