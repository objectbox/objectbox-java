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

package io.objectbox.query;

import io.objectbox.Box;
import io.objectbox.BoxStore;
import io.objectbox.BoxStoreBuilder;
import io.objectbox.DebugFlags;
import io.objectbox.TestEntity;
import io.objectbox.TestEntity_;
import io.objectbox.exception.DbExceptionListener;
import io.objectbox.exception.NonUniqueResultException;
import io.objectbox.query.QueryBuilder.StringOrder;
import io.objectbox.relation.MyObjectBox;
import io.objectbox.relation.Order;
import io.objectbox.relation.Order_;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static io.objectbox.TestEntity_.simpleBoolean;
import static io.objectbox.TestEntity_.simpleByteArray;
import static io.objectbox.TestEntity_.simpleFloat;
import static io.objectbox.TestEntity_.simpleInt;
import static io.objectbox.TestEntity_.simpleLong;
import static io.objectbox.TestEntity_.simpleShort;
import static io.objectbox.TestEntity_.simpleString;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class QueryTest extends AbstractQueryTest {

    @Test
    public void testBuild() {
        Query<TestEntity> query = box.query().build();
        assertNotNull(query);
    }

    @Test(expected = IllegalStateException.class)
    public void testBuildTwice() {
        QueryBuilder<TestEntity> queryBuilder = box.query();
        for (int i = 0; i < 2; i++) {
            // calling any builder method after build should fail
            // note: not calling all variants for different types
            queryBuilder.isNull(TestEntity_.simpleString);
            queryBuilder.and();
            queryBuilder.notNull(TestEntity_.simpleString);
            queryBuilder.or();
            queryBuilder.equal(TestEntity_.simpleBoolean, true);
            queryBuilder.notEqual(TestEntity_.simpleBoolean, true);
            queryBuilder.less(TestEntity_.simpleInt, 42);
            queryBuilder.greater(TestEntity_.simpleInt, 42);
            queryBuilder.between(TestEntity_.simpleInt, 42, 43);
            queryBuilder.in(TestEntity_.simpleInt, new int[]{42});
            queryBuilder.notIn(TestEntity_.simpleInt, new int[]{42});
            queryBuilder.contains(TestEntity_.simpleString, "42");
            queryBuilder.startsWith(TestEntity_.simpleString, "42");
            queryBuilder.order(TestEntity_.simpleInt);
            queryBuilder.build().find();
        }
    }

    @Test
    public void testNullNotNull() {
        List<TestEntity> scalars = putTestEntitiesScalars();
        List<TestEntity> strings = putTestEntitiesStrings();
        assertEquals(strings.size(), box.query().notNull(simpleString).build().count());
        assertEquals(scalars.size(), box.query().isNull(simpleString).build().count());
    }

    @Test
    public void testScalarEqual() {
        putTestEntitiesScalars();

        Query<TestEntity> query = box.query().equal(simpleInt, 2007).build();
        assertEquals(1, query.count());
        assertEquals(8, query.findFirst().getId());
        assertEquals(8, query.findUnique().getId());
        List<TestEntity> all = query.find();
        assertEquals(1, all.size());
        assertEquals(8, all.get(0).getId());
    }

    @Test
    public void testBooleanEqual() {
        putTestEntitiesScalars();

        Query<TestEntity> query = box.query().equal(simpleBoolean, true).build();
        assertEquals(5, query.count());
        assertEquals(1, getFirstNotNull(query).getId());
        query.setParameter(simpleBoolean, false);
        assertEquals(5, query.count());
        assertEquals(2, getFirstNotNull(query).getId());

        // Again, but using alias
        Query<TestEntity> aliasQuery = box.query().equal(simpleBoolean, true).parameterAlias("bool").build();
        assertEquals(5, aliasQuery.count());
        assertEquals(1, getFirstNotNull(aliasQuery).getId());
        aliasQuery.setParameter("bool", false);
        assertEquals(5, aliasQuery.count());
        assertEquals(2, getFirstNotNull(aliasQuery).getId());
    }

    @Test
    public void testNoConditions() {
        List<TestEntity> entities = putTestEntitiesScalars();
        Query<TestEntity> query = box.query().build();
        List<TestEntity> all = query.find();
        assertEquals(entities.size(), all.size());
        assertEquals(entities.size(), query.count());
    }

    @Test
    public void testScalarNotEqual() {
        List<TestEntity> entities = putTestEntitiesScalars();
        Query<TestEntity> query = box.query().notEqual(simpleInt, 2007).notEqual(simpleInt, 2002).build();
        assertEquals(entities.size() - 2, query.count());
    }

    @Test
    public void testScalarLessAndGreater() {
        putTestEntitiesScalars();
        Query<TestEntity> query = box.query().greater(simpleInt, 2003).less(simpleShort, 2107).build();
        assertEquals(3, query.count());
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
        Query<TestEntity> query = box.query().between(simpleInt, 2003, 2006).build();
        assertEquals(4, query.count());
    }

    @Test
    public void testIntIn() {
        putTestEntitiesScalars();

        int[] valuesInt = {1, 1, 2, 3, 2003, 2007, 2002, -1};
        Query<TestEntity> query = box.query().in(simpleInt, valuesInt).parameterAlias("int").build();
        assertEquals(3, query.count());

        int[] valuesInt2 = {2003};
        query.setParameters(simpleInt, valuesInt2);
        assertEquals(1, query.count());

        int[] valuesInt3 = {2003, 2007};
        query.setParameters("int", valuesInt3);
        assertEquals(2, query.count());
    }

    @Test
    public void testLongIn() {
        putTestEntitiesScalars();

        long[] valuesLong = {1, 1, 2, 3, 3003, 3007, 3002, -1};
        Query<TestEntity> query = box.query().in(simpleLong, valuesLong).parameterAlias("long").build();
        assertEquals(3, query.count());

        long[] valuesLong2 = {3003};
        query.setParameters(simpleLong, valuesLong2);
        assertEquals(1, query.count());

        long[] valuesLong3 = {3003, 3007};
        query.setParameters("long", valuesLong3);
        assertEquals(2, query.count());
    }

    @Test
    public void testIntNotIn() {
        putTestEntitiesScalars();

        int[] valuesInt = {1, 1, 2, 3, 2003, 2007, 2002, -1};
        Query<TestEntity> query = box.query().notIn(simpleInt, valuesInt).parameterAlias("int").build();
        assertEquals(7, query.count());

        int[] valuesInt2 = {2003};
        query.setParameters(simpleInt, valuesInt2);
        assertEquals(9, query.count());

        int[] valuesInt3 = {2003, 2007};
        query.setParameters("int", valuesInt3);
        assertEquals(8, query.count());
    }

    @Test
    public void testLongNotIn() {
        putTestEntitiesScalars();

        long[] valuesLong = {1, 1, 2, 3, 3003, 3007, 3002, -1};
        Query<TestEntity> query = box.query().notIn(simpleLong, valuesLong).parameterAlias("long").build();
        assertEquals(7, query.count());

        long[] valuesLong2 = {3003};
        query.setParameters(simpleLong, valuesLong2);
        assertEquals(9, query.count());

        long[] valuesLong3 = {3003, 3007};
        query.setParameters("long", valuesLong3);
        assertEquals(8, query.count());
    }

    @Test
    public void testOffsetLimit() {
        putTestEntitiesScalars();
        Query<TestEntity> query = box.query().greater(simpleInt, 2002).less(simpleShort, 2108).build();
        assertEquals(5, query.count());
        assertEquals(4, query.find(1, 0).size());
        assertEquals(1, query.find(4, 0).size());
        assertEquals(2, query.find(0, 2).size());
        List<TestEntity> list = query.find(1, 2);
        assertEquals(2, list.size());
        assertEquals(2004, list.get(0).getSimpleInt());
        assertEquals(2005, list.get(1).getSimpleInt());
    }

    @Test
    public void testString() {
        List<TestEntity> entities = putTestEntitiesStrings();
        int count = entities.size();
        assertEquals(1, box.query().equal(simpleString, "banana").build().findUnique().getId());
        assertEquals(count - 1, box.query().notEqual(simpleString, "banana").build().count());
        assertEquals(4, box.query().startsWith(simpleString, "ba").endsWith(simpleString, "shake").build().findUnique()
                .getId());
        assertEquals(2, box.query().contains(simpleString, "nana").build().count());
    }

    @Test
    public void testStringLess() {
        putTestEntitiesStrings();
        putTestEntity("BaNaNa Split", 100);
        Query<TestEntity> query = box.query().less(simpleString, "banana juice").order(simpleString).build();
        List<TestEntity> entities = query.find();
        assertEquals(2, entities.size());
        assertEquals("apple", entities.get(0).getSimpleString());
        assertEquals("banana", entities.get(1).getSimpleString());

        query.setParameter(simpleString, "BANANA MZ");
        entities = query.find();
        assertEquals(3, entities.size());
        assertEquals("apple", entities.get(0).getSimpleString());
        assertEquals("banana", entities.get(1).getSimpleString());
        assertEquals("banana milk shake", entities.get(2).getSimpleString());

        // Case sensitive
        query = box.query().less(simpleString, "BANANA", StringOrder.CASE_SENSITIVE).order(simpleString).build();
        assertEquals(0, query.count());

        query.setParameter(simpleString, "banana a");
        entities = query.find();
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
        Query<TestEntity> query = box.query().greater(simpleString, "banana juice").order(simpleString).build();
        List<TestEntity> entities = query.find();
        assertEquals(4, entities.size());
        assertEquals("banana milk shake", entities.get(0).getSimpleString());
        assertEquals("bar", entities.get(1).getSimpleString());
        assertEquals("FOO", entities.get(2).getSimpleString());
        assertEquals("foo bar", entities.get(3).getSimpleString());

        query.setParameter(simpleString, "FO");
        entities = query.find();
        assertEquals(2, entities.size());
        assertEquals("FOO", entities.get(0).getSimpleString());
        assertEquals("foo bar", entities.get(1).getSimpleString());

        // Case sensitive
        query = box.query().greater(simpleString, "banana", StringOrder.CASE_SENSITIVE).order(simpleString).build();
        entities = query.find();
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
        Query<TestEntity> query = box.query().in(simpleString, values).order(simpleString, OrderFlags.CASE_SENSITIVE)
                .build();
        List<TestEntity> entities = query.find();
        assertEquals(3, entities.size());
        assertEquals("BAR", entities.get(0).getSimpleString());
        assertEquals("bar", entities.get(1).getSimpleString());
        assertEquals("foo bar", entities.get(2).getSimpleString());

        String[] values2 = {"bar"};
        query.setParameters(simpleString, values2);
        entities = query.find();
        assertEquals(2, entities.size());
        assertEquals("BAR", entities.get(0).getSimpleString());
        assertEquals("bar", entities.get(1).getSimpleString());

        // Case sensitive
        query = box.query().in(simpleString, values, StringOrder.CASE_SENSITIVE).order(simpleString).build();
        entities = query.find();
        assertEquals(2, entities.size());
        assertEquals("bar", entities.get(0).getSimpleString());
        assertEquals("foo bar", entities.get(1).getSimpleString());
    }

    @Test
    public void testByteArrayEqualsAndSetParameter() {
        putTestEntitiesScalars();

        byte[] value = {1, 2, (byte) 2000};
        Query<TestEntity> query = box.query().equal(simpleByteArray, value).parameterAlias("bytes").build();

        assertEquals(1, query.count());
        TestEntity first = query.findFirst();
        assertNotNull(first);
        assertTrue(Arrays.equals(value, first.getSimpleByteArray()));

        byte[] value2 = {1, 2, (byte) 2001};
        query.setParameter(simpleByteArray, value2);

        assertEquals(1, query.count());
        TestEntity first2 = query.findFirst();
        assertNotNull(first2);
        assertTrue(Arrays.equals(value2, first2.getSimpleByteArray()));

        byte[] value3 = {1, 2, (byte) 2002};
        query.setParameter("bytes", value3);

        assertEquals(1, query.count());
        TestEntity first3 = query.findFirst();
        assertNotNull(first3);
        assertTrue(Arrays.equals(value3, first3.getSimpleByteArray()));
    }

    @Test
    public void byteArray_lessAndGreater_works() {
        putTestEntitiesScalars();
        byte[] value = {1, 2, (byte) 2005};

        // Java does not have compareTo for arrays, so just make sure its not equal to the value.
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
        List<TestEntity> entitiesQueried = box.query().equal(simpleString, sameValueForAll).build().find();
        assertEquals(count, entitiesQueried.size());
    }

    @Test
    public void testEqualStringOrder() {
        putTestEntitiesStrings();
        putTestEntity("BAR", 100);
        assertEquals(2, box.query().equal(simpleString, "bar").build().count());
        assertEquals(1, box.query().equal(simpleString, "bar", StringOrder.CASE_SENSITIVE).build().count());
    }

    @Test
    public void testOrder() {
        putTestEntitiesStrings();
        putTestEntity("BAR", 100);
        List<TestEntity> result = box.query().order(simpleString).build().find();
        assertEquals(6, result.size());
        assertEquals("apple", result.get(0).getSimpleString());
        assertEquals("banana", result.get(1).getSimpleString());
        assertEquals("banana milk shake", result.get(2).getSimpleString());
        assertEquals("bar", result.get(3).getSimpleString());
        assertEquals("BAR", result.get(4).getSimpleString());
        assertEquals("foo bar", result.get(5).getSimpleString());
    }

    @Test
    public void testOrderDescCaseNullLast() {
        putTestEntity(null, 1000);
        putTestEntity("BAR", 100);
        putTestEntitiesStrings();
        int flags = QueryBuilder.CASE_SENSITIVE | QueryBuilder.NULLS_LAST | QueryBuilder.DESCENDING;
        List<TestEntity> result = box.query().order(simpleString, flags).build().find();
        assertEquals(7, result.size());
        assertEquals("foo bar", result.get(0).getSimpleString());
        assertEquals("bar", result.get(1).getSimpleString());
        assertEquals("banana milk shake", result.get(2).getSimpleString());
        assertEquals("banana", result.get(3).getSimpleString());
        assertEquals("apple", result.get(4).getSimpleString());
        assertEquals("BAR", result.get(5).getSimpleString());
        assertNull(result.get(6).getSimpleString());
    }

    @Test
    public void testRemove() {
        putTestEntitiesScalars();
        Query<TestEntity> query = box.query().greater(simpleInt, 2003).build();
        assertEquals(6, query.remove());
        assertEquals(4, box.count());
    }

    @Test
    public void testFindIds() {
        putTestEntitiesScalars();
        assertEquals(10, box.query().build().findIds().length);

        Query<TestEntity> query = box.query().greater(simpleInt, 2006).build();
        long[] keys = query.findIds();
        assertEquals(3, keys.length);
        assertEquals(8, keys[0]);
        assertEquals(9, keys[1]);
        assertEquals(10, keys[2]);
    }

    @Test
    public void testFindIdsWithOrder() {
        putTestEntitiesScalars();
        Query<TestEntity> query = box.query().orderDesc(TestEntity_.simpleInt).build();
        long[] ids = query.findIds();
        assertEquals(10, ids.length);
        assertEquals(10, ids[0]);
        assertEquals(1, ids[9]);

        ids = query.findIds(3, 2);
        assertEquals(2, ids.length);
        assertEquals(7, ids[0]);
        assertEquals(6, ids[1]);
    }

    @Test
    public void testOr() {
        putTestEntitiesScalars();
        Query<TestEntity> query = box.query().equal(simpleInt, 2007).or().equal(simpleLong, 3002).build();
        List<TestEntity> entities = query.find();
        assertEquals(2, entities.size());
        assertEquals(3002, entities.get(0).getSimpleLong());
        assertEquals(2007, entities.get(1).getSimpleInt());
    }

    @Test(expected = IllegalStateException.class)
    public void testOr_bad1() {
        box.query().or();
    }

    @Test(expected = IllegalStateException.class)
    public void testOr_bad2() {
        box.query().equal(simpleInt, 1).or().build();
    }

    @Test
    public void testAnd() {
        putTestEntitiesScalars();
        // OR precedence (wrong): {}, AND precedence (expected): 2008
        Query<TestEntity> query = box.query().equal(simpleInt, 2006).and().equal(simpleInt, 2007).or().equal(simpleInt, 2008).build();
        List<TestEntity> entities = query.find();
        assertEquals(1, entities.size());
        assertEquals(2008, entities.get(0).getSimpleInt());
    }

    @Test(expected = IllegalStateException.class)
    public void testAnd_bad1() {
        box.query().and();
    }

    @Test(expected = IllegalStateException.class)
    public void testAnd_bad2() {
        box.query().equal(simpleInt, 1).and().build();
    }

    @Test(expected = IllegalStateException.class)
    public void testOrAfterAnd() {
        box.query().equal(simpleInt, 1).and().or().equal(simpleInt, 2).build();
    }

    @Test(expected = IllegalStateException.class)
    public void testOrderAfterAnd() {
        box.query().equal(simpleInt, 1).and().order(simpleInt).equal(simpleInt, 2).build();
    }

    @Test
    public void testSetParameterInt() {
        String versionNative = BoxStore.getVersionNative();
        String minVersion = "1.5.1-2018-06-21";
        String versionStart = versionNative.substring(0, minVersion.length());
        assertTrue(versionStart, versionStart.compareTo(minVersion) >= 0);

        putTestEntitiesScalars();
        Query<TestEntity> query = box.query().equal(simpleInt, 2007).parameterAlias("foo").build();
        assertEquals(8, query.findUnique().getId());
        query.setParameter(simpleInt, 2004);
        assertEquals(5, query.findUnique().getId());

        query.setParameter("foo", 2002);
        assertEquals(3, query.findUnique().getId());
    }

    @Test
    public void testSetParameter2Ints() {
        putTestEntitiesScalars();
        Query<TestEntity> query = box.query().between(simpleInt, 2005, 2008).parameterAlias("foo").build();
        assertEquals(4, query.count());
        query.setParameters(simpleInt, 2002, 2003);
        List<TestEntity> entities = query.find();
        assertEquals(2, entities.size());
        assertEquals(3, entities.get(0).getId());
        assertEquals(4, entities.get(1).getId());

        query.setParameters("foo", 2007, 2007);
        assertEquals(8, query.findUnique().getId());
    }

    @Test
    public void testSetParameterFloat() {
        putTestEntitiesScalars();
        Query<TestEntity> query = box.query().greater(simpleFloat, 400.65).parameterAlias("foo").build();
        assertEquals(3, query.count());
        query.setParameter(simpleFloat, 400.75);
        assertEquals(2, query.count());

        query.setParameter("foo", 400.85);
        assertEquals(1, query.count());
    }

    @Test
    public void testSetParameter2Floats() {
        putTestEntitiesScalars();
        Query<TestEntity> query = box.query().between(simpleFloat, 400.15, 400.75).parameterAlias("foo").build();
        assertEquals(6, query.count());
        query.setParameters(simpleFloat, 400.65, 400.85);
        List<TestEntity> entities = query.find();
        assertEquals(2, entities.size());
        assertEquals(8, entities.get(0).getId());
        assertEquals(9, entities.get(1).getId());

        query.setParameters("foo", 400.45, 400.55);
        assertEquals(6, query.findUnique().getId());
    }

    @Test
    public void testSetParameterString() {
        putTestEntitiesStrings();
        Query<TestEntity> query = box.query().equal(simpleString, "banana").parameterAlias("foo").build();
        assertEquals(1, query.findUnique().getId());
        query.setParameter(simpleString, "bar");
        assertEquals(3, query.findUnique().getId());

        assertNull(query.setParameter(simpleString, "not here!").findUnique());

        query.setParameter("foo", "apple");
        assertEquals(2, query.findUnique().getId());
    }

    /**
     * https://github.com/objectbox/objectbox-java/issues/834
     */
    @Test
    public void parameterAlias_combinedConditions() {
        putTestEntitiesScalars();

        Query<TestEntity> query = box.query()
                .greater(simpleInt, 0).parameterAlias("greater")
                .or()
                .less(simpleInt, 0).parameterAlias("less")
                .build();
        List<TestEntity> results = query
                .setParameter("greater", 2008)
                .setParameter("less", 2001)
                .find();
        assertEquals(2, results.size());
        assertEquals(2000, results.get(0).getSimpleInt());
        assertEquals(2009, results.get(1).getSimpleInt());
    }

    @Test
    public void testForEach() {
        List<TestEntity> testEntities = putTestEntitiesStrings();
        final StringBuilder stringBuilder = new StringBuilder();
        box.query().startsWith(simpleString, "banana").build()
                .forEach(data -> stringBuilder.append(data.getSimpleString()).append('#'));
        assertEquals("banana#banana milk shake#", stringBuilder.toString());

        // Verify that box does not hang on to the read-only TX by doing a put
        box.put(new TestEntity());
        assertEquals(testEntities.size() + 1, box.count());
    }

    @Test
    public void testForEachBreak() {
        putTestEntitiesStrings();
        final StringBuilder stringBuilder = new StringBuilder();
        box.query().startsWith(simpleString, "banana").build()
                .forEach(data -> {
                    stringBuilder.append(data.getSimpleString());
                    throw new BreakForEach();
                });
        assertEquals("banana", stringBuilder.toString());
    }

    @Test
    // TODO can we improve? More than just "still works"?
    public void testQueryAttempts() {
        store.close();
        BoxStoreBuilder builder = new BoxStoreBuilder(createTestModel(false)).directory(boxStoreDir)
                .queryAttempts(5)
                .failedReadTxAttemptCallback((result, error) -> error.printStackTrace());
        builder.entity(new TestEntity_());

        store = builder.build();
        putTestEntitiesScalars();

        Query<TestEntity> query = store.boxFor(TestEntity.class).query().equal(simpleInt, 2007).build();
        assertEquals(2007, query.findFirst().getSimpleInt());
    }

    @Test
    public void testDateParam() {
        store.close();
        assertTrue(store.deleteAllFiles());
        store = MyObjectBox.builder().baseDirectory(boxStoreDir).debugFlags(DebugFlags.LOG_QUERY_PARAMETERS).build();

        Date now = new Date();
        Order order = new Order();
        order.setDate(now);
        Box<Order> box = store.boxFor(Order.class);
        box.put(order);

        Query<Order> query = box.query().equal(Order_.date, 0).build();
        assertEquals(0, query.count());

        query.setParameter(Order_.date, now);
        assertEquals(1, query.count());

        // Again, but using alias
        Query<Order> aliasQuery = box.query().equal(Order_.date, 0).parameterAlias("date").build();
        assertEquals(0, aliasQuery.count());

        aliasQuery.setParameter("date", now);
        assertEquals(1, aliasQuery.count());
    }

    @Test
    public void testFailedUnique_exceptionListener() {
        final Exception[] exs = {null};
        DbExceptionListener exceptionListener = e -> exs[0] = e;
        putTestEntitiesStrings();
        Query<TestEntity> query = box.query().build();
        store.setDbExceptionListener(exceptionListener);
        try {
            query.findUnique();
            fail("Should have thrown");
        } catch (NonUniqueResultException e) {
            assertSame(e, exs[0]);
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
        Query<TestEntity> query = box.query().build();
        store.setDbExceptionListener(exceptionListener);

        TestEntity object = query.findUnique();
        assertNull(object);
        assertNotNull(exs[0]);
        assertEquals(exs[0].getClass(), NonUniqueResultException.class);
    }

    @Test
    public void testDescribe() {
        // Note: description string correctness is fully asserted in core library.

        // No conditions.
        Query<TestEntity> queryNoConditions = box.query().build();
        assertEquals("Query for entity TestEntity with 1 conditions", queryNoConditions.describe());
        assertEquals("TRUE", queryNoConditions.describeParameters());

        // Some conditions.
        Query<TestEntity> query = box.query()
                .equal(TestEntity_.simpleString, "Hello")
                .or().greater(TestEntity_.simpleInt, 42)
                .build();
        String describeActual = query.describe();
        assertTrue(describeActual.startsWith("Query for entity TestEntity with 3 conditions with properties "));
        // Note: the order properties are listed in is not fixed.
        assertTrue(describeActual.contains(TestEntity_.simpleString.name));
        assertTrue(describeActual.contains(TestEntity_.simpleInt.name));
        assertEquals("(simpleString ==(i) \"Hello\"\n OR simpleInt > 42)", query.describeParameters());
    }

    private <T> void buildFindAndAssert(QueryBuilder<T> builder, int expectedCount, ListItemAsserter<T> asserter) {
        List<T> results = builder.build().find();
        assertEquals(expectedCount, results.size());
        for (int i = 0; i < results.size(); i++) {
            asserter.assertListItem(i, results.get(i));
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
}
