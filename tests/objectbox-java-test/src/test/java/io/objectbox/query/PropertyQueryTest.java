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

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import io.objectbox.TestEntity;
import io.objectbox.TestEntityCursor;
import io.objectbox.exception.DbException;
import io.objectbox.exception.NumericOverflowException;
import io.objectbox.query.QueryBuilder.StringOrder;


import static io.objectbox.TestEntity_.simpleBoolean;
import static io.objectbox.TestEntity_.simpleByte;
import static io.objectbox.TestEntity_.simpleByteArray;
import static io.objectbox.TestEntity_.simpleDouble;
import static io.objectbox.TestEntity_.simpleFloat;
import static io.objectbox.TestEntity_.simpleInt;
import static io.objectbox.TestEntity_.simpleIntU;
import static io.objectbox.TestEntity_.simpleLong;
import static io.objectbox.TestEntity_.simpleLongU;
import static io.objectbox.TestEntity_.simpleShort;
import static io.objectbox.TestEntity_.simpleShortU;
import static io.objectbox.TestEntity_.simpleString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class PropertyQueryTest extends AbstractQueryTest {

    private void putTestEntityInteger(byte vByte, short vShort, int vInt, long vLong) {
        TestEntity entity = new TestEntity();
        entity.setSimpleByte(vByte);
        entity.setSimpleShort(vShort);
        entity.setSimpleInt(vInt);
        entity.setSimpleLong(vLong);
        entity.setSimpleShortU(vShort);
        entity.setSimpleIntU(vInt);
        entity.setSimpleLongU(vLong);
        box.put(entity);
    }

    private void putTestEntityUnsignedInteger(short vShort, int vInt, long vLong) {
        TestEntity entity = new TestEntity();
        entity.setSimpleShortU(vShort);
        entity.setSimpleIntU(vInt);
        entity.setSimpleLongU(vLong);
        box.put(entity);
    }

    private void putTestEntityFloat(float vFloat, double vDouble) {
        TestEntity entity = new TestEntity();
        entity.setSimpleFloat(vFloat);
        entity.setSimpleDouble(vDouble);
        box.put(entity);
    }

    @Test
    public void testFindStrings() {
        putTestEntity(null, 1000);
        putTestEntity("BAR", 100);
        putTestEntitiesStrings();
        putTestEntity("banana", 101);
        Query<TestEntity> query = box.query().startsWith(simpleString, "b").build();

        String[] result = query.property(simpleString).findStrings();
        assertEquals(5, result.length);
        assertEquals("BAR", result[0]);
        assertEquals("banana", result[1]);
        assertEquals("bar", result[2]);
        assertEquals("banana milk shake", result[3]);
        assertEquals("banana", result[4]);

        result = query.property(simpleString).distinct().findStrings();
        assertEquals(3, result.length);
        List<String> list = Arrays.asList(result);
        assertTrue(list.contains("BAR"));
        assertTrue(list.contains("banana"));
        assertTrue(list.contains("banana milk shake"));

        result = query.property(simpleString).distinct(StringOrder.CASE_SENSITIVE).findStrings();
        assertEquals(4, result.length);
        list = Arrays.asList(result);
        assertTrue(list.contains("BAR"));
        assertTrue(list.contains("banana"));
        assertTrue(list.contains("bar"));
        assertTrue(list.contains("banana milk shake"));
    }

    @Test
    public void testFindStrings_nullValue() {
        putTestEntity(null, 3);
        putTestEntitiesStrings();
        Query<TestEntity> query = box.query().equal(simpleInt, 3).build();

        String[] strings = query.property(simpleString).findStrings();
        assertEquals(1, strings.length);
        assertEquals("bar", strings[0]);

        strings = query.property(simpleString).nullValue("****").findStrings();
        assertEquals(2, strings.length);
        assertEquals("****", strings[0]);
        assertEquals("bar", strings[1]);

        putTestEntity(null, 3);

        assertEquals(3, query.property(simpleString).nullValue("****").findStrings().length);
        assertEquals(2, query.property(simpleString).nullValue("****").distinct().findStrings().length);
    }

    @Test
    public void testFindInts_nullValue() {
        putTestEntity(null, 1);
        TestEntityCursor.INT_NULL_HACK = true;
        try {
            putTestEntities(3);
        } finally {
            TestEntityCursor.INT_NULL_HACK = false;
        }
        Query<TestEntity> query = box.query().equal(simpleLong, 1001).build();

        int[] results = query.property(simpleInt).findInts();
        assertEquals(1, results.length);
        assertEquals(1, results[0]);

        results = query.property(simpleInt).nullValue(-1977).findInts();
        assertEquals(2, results.length);
        assertEquals(1, results[0]);
        assertEquals(-1977, results[1]);
    }

    // TODO add null tests for other types

    @Test(expected = IllegalArgumentException.class)
    public void testFindStrings_wrongPropertyType() {
        putTestEntitiesStrings();
        box.query().build().property(simpleInt).findStrings();
    }

    @Test
    public void testFindString() {
        Query<TestEntity> query = box.query().greater(simpleLong, 1002).build();
        PropertyQuery propertyQuery = query.property(simpleString);
        assertNull(propertyQuery.findString());
        assertNull(propertyQuery.reset().unique().findString());
        putTestEntities(5);
        assertEquals("foo3", propertyQuery.reset().findString());

        query = box.query().greater(simpleLong, 1004).build();
        propertyQuery = query.property(simpleString);
        assertEquals("foo5", propertyQuery.reset().unique().findString());

        putTestEntity(null, 6);
        putTestEntity(null, 7);
        query.setParameter(simpleLong, 1005);
        assertEquals("nope", propertyQuery.reset().distinct().nullValue("nope").unique().findString());
    }

    @Test(expected = DbException.class)
    public void testFindString_uniqueFails() {
        putTestEntity("foo", 1);
        putTestEntity("foo", 2);
        box.query().build().property(simpleString).unique().findString();
    }

    @Test
    public void testFindLongs() {
        putTestEntities(5);
        Query<TestEntity> query = box.query().greater(simpleLong, 1002).build();
        long[] result = query.property(simpleLong).findLongs();
        assertEquals(3, result.length);
        assertEquals(1003, result[0]);
        assertEquals(1004, result[1]);
        assertEquals(1005, result[2]);

        putTestEntity(null, 5);

        query = box.query().greater(simpleLong, 1004).build();
        assertEquals(2, query.property(simpleLong).findLongs().length);
        assertEquals(1, query.property(simpleLong).distinct().findLongs().length);
    }

    @Test
    public void testFindLong() {
        Query<TestEntity> query = box.query().greater(simpleLong, 1002).build();
        assertNull(query.property(simpleLong).findLong());
        assertNull(query.property(simpleLong).findLong());
        putTestEntities(5);
        assertEquals(1003, (long) query.property(simpleLong).findLong());

        query = box.query().greater(simpleLong, 1004).build();
        assertEquals(1005, (long) query.property(simpleLong).distinct().findLong());
    }

    @Test(expected = DbException.class)
    public void testFindLong_uniqueFails() {
        putTestEntity(null, 1);
        putTestEntity(null, 1);
        box.query().build().property(simpleLong).unique().findLong();
    }

    @Test
    public void testFindInt() {
        Query<TestEntity> query = box.query().greater(simpleLong, 1002).build();
        assertNull(query.property(simpleInt).findInt());
        assertNull(query.property(simpleInt).unique().findInt());
        putTestEntities(5);
        assertEquals(3, (int) query.property(simpleInt).findInt());

        query = box.query().greater(simpleLong, 1004).build();
        assertEquals(5, (int) query.property(simpleInt).distinct().unique().findInt());

        TestEntityCursor.INT_NULL_HACK = true;
        try {
            putTestEntity(null, 6);
        } finally {
            TestEntityCursor.INT_NULL_HACK = false;
        }
        query.setParameter(simpleLong, 1005);
        assertEquals(-99, (int) query.property(simpleInt).nullValue(-99).unique().findInt());
    }

    @Test(expected = DbException.class)
    public void testFindInt_uniqueFails() {
        putTestEntity(null, 1);
        putTestEntity(null, 1);
        box.query().build().property(simpleInt).unique().findInt();
    }

    @Test
    public void testFindShort() {
        Query<TestEntity> query = box.query().greater(simpleLong, 1002).build();
        assertNull(query.property(simpleShort).findShort());
        assertNull(query.property(simpleShort).unique().findShort());

        putTestEntities(5);
        assertEquals(103, (short) query.property(simpleShort).findShort());

        query = box.query().greater(simpleLong, 1004).build();
        assertEquals(105, (short) query.property(simpleShort).distinct().unique().findShort());
    }

    @Test(expected = DbException.class)
    public void testFindShort_uniqueFails() {
        putTestEntity(null, 1);
        putTestEntity(null, 1);
        box.query().build().property(simpleShort).unique().findShort();
    }

    // TODO add test for findChar

    @Test
    public void testFindByte() {
        Query<TestEntity> query = box.query().greater(simpleLong, 1002).build();
        assertNull(query.property(simpleByte).findByte());
        assertNull(query.property(simpleByte).unique().findByte());

        putTestEntities(5);
        assertEquals((byte) 13, (byte) query.property(simpleByte).findByte());

        query = box.query().greater(simpleLong, 1004).build();
        assertEquals((byte) 15, (byte) query.property(simpleByte).distinct().unique().findByte());
    }

    @Test(expected = DbException.class)
    public void testFindByte_uniqueFails() {
        putTestEntity(null, 1);
        putTestEntity(null, 1);
        box.query().build().property(simpleByte).unique().findByte();
    }

    @Test
    public void testFindBoolean() {
        Query<TestEntity> query = box.query().greater(simpleLong, 1002).build();
        assertNull(query.property(simpleBoolean).findBoolean());
        assertNull(query.property(simpleBoolean).unique().findBoolean());

        putTestEntities(5);
        assertFalse(query.property(simpleBoolean).findBoolean());

        query = box.query().greater(simpleLong, 1004).build();
        assertFalse(query.property(simpleBoolean).distinct().unique().findBoolean());
    }

    @Test(expected = DbException.class)
    public void testFindBoolean_uniqueFails() {
        putTestEntity(null, 1);
        putTestEntity(null, 1);
        box.query().build().property(simpleBoolean).unique().findBoolean();
    }

    @Test
    public void testFindFloat() {
        Query<TestEntity> query = box.query().greater(simpleLong, 1002).build();
        assertNull(query.property(simpleFloat).findFloat());
        assertNull(query.property(simpleFloat).unique().findFloat());

        putTestEntities(5);
        assertEquals(200.3f, query.property(simpleFloat).findFloat(), 0.001f);

        query = box.query().greater(simpleLong, 1004).build();
        assertEquals(200.5f, query.property(simpleFloat).distinct().unique().findFloat(), 0.001f);
    }

    @Test(expected = DbException.class)
    public void testFindFloat_uniqueFails() {
        putTestEntity(null, 1);
        putTestEntity(null, 1);
        box.query().build().property(simpleFloat).unique().findFloat();
    }

    @Test
    public void testFindDouble() {
        Query<TestEntity> query = box.query().greater(simpleLong, 1002).build();
        assertNull(query.property(simpleDouble).findDouble());
        assertNull(query.property(simpleDouble).unique().findDouble());
        putTestEntities(5);
        assertEquals(2000.03, query.property(simpleDouble).findDouble(), 0.001);

        query = box.query().greater(simpleLong, 1004).build();
        assertEquals(2000.05, query.property(simpleDouble).distinct().unique().findDouble(), 0.001);
    }

    @Test(expected = DbException.class)
    public void testFindDouble_uniqueFails() {
        putTestEntity(null, 1);
        putTestEntity(null, 1);
        box.query().build().property(simpleDouble).unique().findDouble();
    }

    @Test
    public void testFindInts() {
        putTestEntities(5);
        Query<TestEntity> query = box.query().greater(simpleInt, 2).build();
        int[] result = query.property(simpleInt).findInts();
        assertEquals(3, result.length);
        assertEquals(3, result[0]);
        assertEquals(4, result[1]);
        assertEquals(5, result[2]);

        putTestEntity(null, 5);

        query = box.query().greater(simpleInt, 4).build();
        assertEquals(2, query.property(simpleInt).findInts().length);
        assertEquals(1, query.property(simpleInt).distinct().findInts().length);
    }

    @Test
    public void testFindShorts() {
        putTestEntities(5);
        Query<TestEntity> query = box.query().greater(simpleInt, 2).build();
        short[] result = query.property(simpleShort).findShorts();
        assertEquals(3, result.length);
        assertEquals(103, result[0]);
        assertEquals(104, result[1]);
        assertEquals(105, result[2]);

        putTestEntity(null, 5);

        query = box.query().greater(simpleInt, 4).build();
        assertEquals(2, query.property(simpleShort).findShorts().length);
        assertEquals(1, query.property(simpleShort).distinct().findShorts().length);
    }

    // TODO @Test for findChars (no char property in entity)

    @Test
    public void testFindFloats() {
        putTestEntities(5);
        Query<TestEntity> query = box.query().greater(simpleInt, 2).build();
        float[] result = query.property(simpleFloat).findFloats();
        assertEquals(3, result.length);
        assertEquals(200.3f, result[0], 0.0001f);
        assertEquals(200.4f, result[1], 0.0001f);
        assertEquals(200.5f, result[2], 0.0001f);

        putTestEntity(null, 5);

        query = box.query().greater(simpleInt, 4).build();
        assertEquals(2, query.property(simpleFloat).findFloats().length);
        assertEquals(1, query.property(simpleFloat).distinct().findFloats().length);
    }

    @Test
    public void testFindDoubles() {
        putTestEntities(5);
        Query<TestEntity> query = box.query().greater(simpleInt, 2).build();
        double[] result = query.property(simpleDouble).findDoubles();
        assertEquals(3, result.length);
        assertEquals(2000.03, result[0], 0.0001);
        assertEquals(2000.04, result[1], 0.0001);
        assertEquals(2000.05, result[2], 0.0001);

        putTestEntity(null, 5);

        query = box.query().greater(simpleInt, 4).build();
        assertEquals(2, query.property(simpleDouble).findDoubles().length);
        assertEquals(1, query.property(simpleDouble).distinct().findDoubles().length);
    }

    @Test
    public void testFindBytes() {
        putTestEntities(5);
        Query<TestEntity> query = box.query().greater(simpleByte, 12).build();
        byte[] result = query.property(simpleByte).findBytes();
        assertEquals(3, result.length);
        assertEquals(13, result[0]);
        assertEquals(14, result[1]);
        assertEquals(15, result[2]);

        putTestEntity(null, 5);

        query = box.query().greater(simpleByte, 14).build();
        assertEquals(2, query.property(simpleByte).findBytes().length);
        assertEquals(1, query.property(simpleByte).distinct().findBytes().length);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFindLongs_wrongPropertyType() {
        putTestEntitiesStrings();
        box.query().build().property(simpleInt).findLongs();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFindInts_wrongPropertyType() {
        putTestEntitiesStrings();
        box.query().build().property(simpleLong).findInts();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFindShorts_wrongPropertyType() {
        putTestEntitiesStrings();
        box.query().build().property(simpleInt).findShorts();
    }

    @Test
    public void testCount() {
        Query<TestEntity> query = box.query().build();
        PropertyQuery stringQuery = query.property(simpleString);

        assertEquals(0, stringQuery.count());

        putTestEntity(null, 1000);
        putTestEntity("BAR", 100);
        putTestEntitiesStrings();
        putTestEntity("banana", 101);

        assertEquals(8, query.count());
        assertEquals(7, stringQuery.count());
        assertEquals(6, stringQuery.distinct().count());
    }

    private void assertUnsupported(Runnable runnable, String exceptionMessage) {
        try {
            runnable.run();
            fail("Should have thrown IllegalArgumentException: " + exceptionMessage);
        } catch (Exception e) {
            assertTrue(
                    "Expected IllegalStateException, but was " + e.getClass().getSimpleName() + ".",
                    e instanceof IllegalStateException
            );
            assertTrue(
                    "Expected exception message '" + exceptionMessage + "', but was '" + e.getMessage() + "'.",
                    e.getMessage().contains(exceptionMessage)
            );
        }
    }

    @Test
    public void avg_notSupported() {
        Query<TestEntity> query = box.query().build();
        String exceptionMessage = "Cannot calculate sum. This function is for integer types only. This operation is not supported for Property ";
        assertUnsupported(() -> query.property(simpleByteArray).avg(), exceptionMessage);
        assertUnsupported(() -> query.property(simpleString).avg(), exceptionMessage);
    }

    @Test
    public void avgLong_notSupported() {
        Query<TestEntity> query = box.query().build();
        String exceptionMessage = "Cannot calculate sum. This function is for integer types only. This operation is not supported for Property ";
        assertUnsupported(() -> query.property(simpleByteArray).avgLong(), exceptionMessage);
        assertUnsupported(() -> query.property(simpleString).avgLong(), exceptionMessage);

        String exceptionMessage2 = "Please use the double based average instead. This operation is not supported for Property ";
        assertUnsupported(() -> query.property(simpleFloat).avgLong(), exceptionMessage2);
        assertUnsupported(() -> query.property(simpleDouble).avgLong(), exceptionMessage2);
    }

    @Test
    public void min_notSupported() {
        Query<TestEntity> query = box.query().build();
        String exceptionMessage = "This operation is not supported for Property ";
        assertUnsupported(() -> query.property(simpleBoolean).min(), exceptionMessage);
        assertUnsupported(() -> query.property(simpleByteArray).min(), exceptionMessage);
        assertUnsupported(() -> query.property(simpleString).min(), exceptionMessage);

        String exceptionMessage2 = "Use double based min (e.g. `minDouble()`) instead. This operation is not supported for Property ";
        assertUnsupported(() -> query.property(simpleFloat).min(), exceptionMessage2);
        assertUnsupported(() -> query.property(simpleDouble).min(), exceptionMessage2);
    }

    @Test
    public void minDouble_notSupported() {
        Query<TestEntity> query = box.query().build();
        String exceptionMessage = "Not a floating point type. This operation is not supported for Property ";
        assertUnsupported(() -> query.property(simpleBoolean).minDouble(), exceptionMessage);
        assertUnsupported(() -> query.property(simpleByteArray).minDouble(), exceptionMessage);
        assertUnsupported(() -> query.property(simpleString).minDouble(), exceptionMessage);

        assertUnsupported(() -> query.property(simpleByte).minDouble(), exceptionMessage);
        assertUnsupported(() -> query.property(simpleShort).minDouble(), exceptionMessage);
        assertUnsupported(() -> query.property(simpleInt).minDouble(), exceptionMessage);
        assertUnsupported(() -> query.property(simpleLong).minDouble(), exceptionMessage);
        assertUnsupported(() -> query.property(simpleShortU).minDouble(), exceptionMessage);
        assertUnsupported(() -> query.property(simpleIntU).minDouble(), exceptionMessage);
        assertUnsupported(() -> query.property(simpleLongU).minDouble(), exceptionMessage);
    }

    @Test
    public void max_notSupported() {
        Query<TestEntity> query = box.query().build();
        String exceptionMessage = "This operation is not supported for Property ";
        assertUnsupported(() -> query.property(simpleBoolean).max(), exceptionMessage);
        assertUnsupported(() -> query.property(simpleByteArray).max(), exceptionMessage);
        assertUnsupported(() -> query.property(simpleString).max(), exceptionMessage);

        String exceptionMessage2 = "Use double based max (e.g. `maxDouble()`) instead. This operation is not supported for Property ";
        assertUnsupported(() -> query.property(simpleFloat).max(), exceptionMessage2);
        assertUnsupported(() -> query.property(simpleDouble).max(), exceptionMessage2);
    }

    @Test
    public void maxDouble_notSupported() {
        Query<TestEntity> query = box.query().build();
        String exceptionMessage = "Not a floating point type. This operation is not supported for Property ";
        assertUnsupported(() -> query.property(simpleBoolean).maxDouble(), exceptionMessage);
        assertUnsupported(() -> query.property(simpleByteArray).maxDouble(), exceptionMessage);
        assertUnsupported(() -> query.property(simpleString).maxDouble(), exceptionMessage);

        assertUnsupported(() -> query.property(simpleByte).maxDouble(), exceptionMessage);
        assertUnsupported(() -> query.property(simpleShort).maxDouble(), exceptionMessage);
        assertUnsupported(() -> query.property(simpleInt).maxDouble(), exceptionMessage);
        assertUnsupported(() -> query.property(simpleLong).maxDouble(), exceptionMessage);
        assertUnsupported(() -> query.property(simpleShortU).maxDouble(), exceptionMessage);
        assertUnsupported(() -> query.property(simpleIntU).maxDouble(), exceptionMessage);
        assertUnsupported(() -> query.property(simpleLongU).maxDouble(), exceptionMessage);
    }

    @Test
    public void sum_notSupported() {
        Query<TestEntity> query = box.query().build();
        String exceptionMessage = "Cannot calculate sum. This function is for integer types only. This operation is not supported for Property ";
        assertUnsupported(() -> query.property(simpleByteArray).sum(), exceptionMessage);
        assertUnsupported(() -> query.property(simpleString).sum(), exceptionMessage);

        String exceptionMessage2 = "Please use the double based sum instead. This operation is not supported for Property ";
        assertUnsupported(() -> query.property(simpleFloat).sum(), exceptionMessage2);
        assertUnsupported(() -> query.property(simpleDouble).sum(), exceptionMessage2);
    }

    @Test
    public void avg_noData() {
        Query<TestEntity> baseQuery = box.query().build();
        // Integer.
        assertEquals(Double.NaN, baseQuery.property(simpleByte).avg(), 0.0);
        assertEquals(Double.NaN, baseQuery.property(simpleShort).avg(), 0.0);
        assertEquals(Double.NaN, baseQuery.property(simpleInt).avg(), 0.0);
        assertEquals(Double.NaN, baseQuery.property(simpleLong).avg(), 0.0);
        // Integer treated as unsigned.
        assertEquals(Double.NaN, baseQuery.property(simpleShortU).avg(), 0.0);
        assertEquals(Double.NaN, baseQuery.property(simpleIntU).avg(), 0.0);
        assertEquals(Double.NaN, baseQuery.property(simpleLongU).avg(), 0.0);
        // Float.
        assertEquals(Double.NaN, baseQuery.property(simpleFloat).avg(), 0.0);
        assertEquals(Double.NaN, baseQuery.property(simpleDouble).avg(), 0.0);
    }

    @Test
    public void avgLong_noData() {
        Query<TestEntity> baseQuery = box.query().build();
        // Integer.
        assertEquals(0, baseQuery.property(simpleByte).avgLong());
        assertEquals(0, baseQuery.property(simpleShort).avgLong());
        assertEquals(0, baseQuery.property(simpleInt).avgLong());
        assertEquals(0, baseQuery.property(simpleLong).avgLong());
        // Integer treated as unsigned.
        assertEquals(0, baseQuery.property(simpleShortU).avgLong());
        assertEquals(0, baseQuery.property(simpleIntU).avgLong());
        assertEquals(0, baseQuery.property(simpleLongU).avgLong());
    }

    @Test
    public void min_noData() {
        Query<TestEntity> baseQuery = box.query().build();
        assertEquals(Long.MAX_VALUE, baseQuery.property(simpleByte).min());
        assertEquals(Long.MAX_VALUE, baseQuery.property(simpleShort).min());
        assertEquals(Long.MAX_VALUE, baseQuery.property(simpleInt).min());
        assertEquals(Long.MAX_VALUE, baseQuery.property(simpleLong).min());
        // Integer treated as unsigned.
        assertEquals(Long.MAX_VALUE, baseQuery.property(simpleShortU).min());
        assertEquals(Long.MAX_VALUE, baseQuery.property(simpleIntU).min());
        assertEquals(Long.MAX_VALUE, baseQuery.property(simpleLongU).min());
    }

    @Test
    public void minDouble_noData() {
        Query<TestEntity> baseQuery = box.query().build();
        assertEquals(Double.NaN, baseQuery.property(simpleFloat).minDouble(), 0.0001);
        assertEquals(Double.NaN, baseQuery.property(simpleDouble).minDouble(), 0.0001);
    }

    @Test
    public void max_noData() {
        Query<TestEntity> baseQuery = box.query().build();
        assertEquals(Long.MIN_VALUE, baseQuery.property(simpleByte).max());
        assertEquals(Long.MIN_VALUE, baseQuery.property(simpleShort).max());
        assertEquals(Long.MIN_VALUE, baseQuery.property(simpleInt).max());
        assertEquals(Long.MIN_VALUE, baseQuery.property(simpleLong).max());
        // Integer treated as unsigned.
        assertEquals(Long.MIN_VALUE, baseQuery.property(simpleShortU).max());
        assertEquals(Long.MIN_VALUE, baseQuery.property(simpleIntU).max());
        assertEquals(Long.MIN_VALUE, baseQuery.property(simpleLongU).max());
    }

    @Test
    public void maxDouble_noData() {
        Query<TestEntity> baseQuery = box.query().build();
        assertEquals(Double.NaN, baseQuery.property(simpleFloat).maxDouble(), 0.0001);
        assertEquals(Double.NaN, baseQuery.property(simpleDouble).maxDouble(), 0.0001);
    }

    @Test
    public void sum_noData() {
        Query<TestEntity> baseQuery = box.query().build();
        assertEquals(0, baseQuery.property(simpleByte).sum());
        assertEquals(0, baseQuery.property(simpleShort).sum());
        assertEquals(0, baseQuery.property(simpleInt).sum());
        assertEquals(0, baseQuery.property(simpleLong).sum());
        // Integer treated as unsigned.
        assertEquals(0, baseQuery.property(simpleShortU).sum());
        assertEquals(0, baseQuery.property(simpleIntU).sum());
        assertEquals(0, baseQuery.property(simpleLongU).sum());
    }

    @Test
    public void sumDouble_noData() {
        Query<TestEntity> baseQuery = box.query().build();
        // Integer.
        assertEquals(0, baseQuery.property(simpleByte).sumDouble(), 0.0001);
        assertEquals(0, baseQuery.property(simpleInt).sumDouble(), 0.0001);
        assertEquals(0, baseQuery.property(simpleShort).sumDouble(), 0.0001);
        assertEquals(0, baseQuery.property(simpleLong).sumDouble(), 0.0001);
        // Integer treated as unsigned.
        assertEquals(0, baseQuery.property(simpleIntU).sumDouble(), 0.0001);
        assertEquals(0, baseQuery.property(simpleShortU).sumDouble(), 0.0001);
        assertEquals(0, baseQuery.property(simpleLongU).sumDouble(), 0.0001);
        // Floating point.
        assertEquals(0, baseQuery.property(simpleFloat).sumDouble(), 0.0001);
        assertEquals(0, baseQuery.property(simpleDouble).sumDouble(), 0.0001);
    }

    @Test
    public void avg_positiveOverflow() {
        putTestEntityFloat(Float.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
        putTestEntityFloat(1, 1);

        Query<TestEntity> baseQuery = box.query().build();
        assertEquals(Float.POSITIVE_INFINITY, baseQuery.property(simpleFloat).avg(), 0.001);
        assertEquals(Double.POSITIVE_INFINITY, baseQuery.property(simpleDouble).avg(), 0.001);
    }

    @Test
    public void avg_negativeOverflow() {
        putTestEntityFloat(Float.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);
        putTestEntityFloat(-1, -1);

        Query<TestEntity> baseQuery = box.query().build();
        assertEquals(Float.NEGATIVE_INFINITY, baseQuery.property(simpleFloat).avg(), 0.001);
        assertEquals(Double.NEGATIVE_INFINITY, baseQuery.property(simpleDouble).avg(), 0.001);
    }

    @Test
    public void avg_NaN() {
        putTestEntityFloat(Float.NaN, Double.NaN);
        putTestEntityFloat(1, 1);

        Query<TestEntity> baseQuery = box.query().build();
        assertEquals(Float.NaN, baseQuery.property(simpleFloat).avg(), 0.001);
        assertEquals(Double.NaN, baseQuery.property(simpleDouble).avg(), 0.001);
    }

    @Test
    public void avgLong_positiveOverflow() {
        putTestEntityInteger((byte) 0, (short) 0, 0, Long.MAX_VALUE);
        putTestEntityInteger((byte) 0, (short) 0, 0, 1);

        Query<TestEntity> baseQuery = box.query().build();
        assertEquals(Long.MAX_VALUE / 2 + 1, baseQuery.property(simpleLong).avgLong());
        // Should not change if treated as unsigned.
        assertEquals(Long.MAX_VALUE / 2 + 1, baseQuery.property(simpleLongU).avgLong());
    }

    @Test
    public void avgLong_negativeOverflow() {
        putTestEntityInteger((byte) 0, (short) 0, 0, Long.MIN_VALUE);
        putTestEntityInteger((byte) 0, (short) 0, 0, -1);

        Query<TestEntity> baseQuery = box.query().build();
        assertEquals(Long.MIN_VALUE / 2, baseQuery.property(simpleLong).avgLong());
        // Should not change if treated as unsigned.
        assertEquals(Long.MIN_VALUE / 2, baseQuery.property(simpleLongU).avgLong());
    }

    @Test
    public void avgLong_unsignedOverflow() {
        putTestEntityInteger((byte) 0, (short) 0, 0, -1);
        putTestEntityInteger((byte) 0, (short) 0, 0, 1);

        Query<TestEntity> baseQuery = box.query().build();
        assertEquals(Long.MIN_VALUE, baseQuery.property(simpleLongU).avgLong());
        // Should be different if treated as signed.
        assertEquals(0, baseQuery.property(simpleLong).avgLong());
    }

    @Test
    public void sum_byteShortIntOverflow() {
        putTestEntityInteger(Byte.MAX_VALUE, Short.MAX_VALUE, Integer.MAX_VALUE, 0);
        putTestEntityInteger((byte) 1, (short) 1, 1, 0);

        Query<TestEntity> baseQuery = box.query().build();
        assertEquals(Byte.MAX_VALUE + 1, baseQuery.property(simpleByte).sum());
        assertEquals(Short.MAX_VALUE + 1, baseQuery.property(simpleShort).sum());
        assertEquals(Integer.MAX_VALUE + 1L, baseQuery.property(simpleInt).sum());
    }

    @Test
    public void sum_unsignedShortIntOverflow() {
        putTestEntityUnsignedInteger((short) -1, -1, 0);
        putTestEntityUnsignedInteger((short) 1, 1, 0);

        Query<TestEntity> baseQuery = box.query().build();
        assertEquals(0x1_0000, baseQuery.property(simpleShortU).sum());
        assertEquals(0x1_0000_0000L, baseQuery.property(simpleIntU).sum());
    }

    @Test
    public void sum_longOverflow_exception() {
        putTestEntityInteger((byte) 0, (short) 0, 0, Long.MAX_VALUE);
        putTestEntityInteger((byte) 0, (short) 0, 0, 1);

        NumericOverflowException exception = assertThrows(NumericOverflowException.class, () ->
                box.query().build().property(simpleLong).sum()
        );
        assertTrue(exception.getMessage().contains("Numeric overflow"));
    }

    @Test
    public void sum_longUnderflow_exception() {
        putTestEntityInteger((byte) 0, (short) 0, 0, Long.MIN_VALUE);
        putTestEntityInteger((byte) 0, (short) 0, 0, -1);

        NumericOverflowException exception = assertThrows(NumericOverflowException.class, () ->
                box.query().build().property(simpleLong).sum()
        );
        assertTrue(exception.getMessage().contains("Numeric overflow"));
    }

    @Test
    public void sum_unsignedLongOverflow_exception() {
        putTestEntityUnsignedInteger((short) 0, 0, -1);
        putTestEntityUnsignedInteger((short) 0, 0, 1);

        NumericOverflowException exception = assertThrows(NumericOverflowException.class, () ->
                box.query().build().property(simpleLongU).sum()
        );
        assertTrue(exception.getMessage().contains("Numeric overflow"));
    }

    @Test
    public void sumDouble_positiveInfinity() {
        putTestEntityFloat(Float.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
        putTestEntityFloat(1, 1);

        Query<TestEntity> baseQuery = box.query().build();
        assertEquals(Float.POSITIVE_INFINITY, baseQuery.property(simpleFloat).avg(), 0.001);
        assertEquals(Double.POSITIVE_INFINITY, baseQuery.property(simpleDouble).avg(), 0.001);
    }

    @Test
    public void sumDouble_negativeInfinity() {
        putTestEntityFloat(Float.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);
        putTestEntityFloat(-1, -1);

        Query<TestEntity> baseQuery = box.query().build();
        assertEquals(Float.NEGATIVE_INFINITY, baseQuery.property(simpleFloat).avg(), 0.001);
        assertEquals(Double.NEGATIVE_INFINITY, baseQuery.property(simpleDouble).avg(), 0.001);
    }

    @Test
    public void sumDouble_NaN() {
        putTestEntityFloat(Float.NaN, Double.NaN);
        putTestEntityFloat(1, 1);

        Query<TestEntity> baseQuery = box.query().build();
        assertEquals(Float.NaN, baseQuery.property(simpleFloat).sumDouble(), 0.001);
        assertEquals(Double.NaN, baseQuery.property(simpleDouble).sumDouble(), 0.001);
    }

    @Test
    public void testAggregates() {
        putTestEntitiesScalars();
        Query<TestEntity> query = box.query().less(simpleInt, 2002).build(); // 2 results.
        PropertyQuery booleanQuery = query.property(simpleBoolean);
        PropertyQuery byteQuery = query.property(simpleByte);
        PropertyQuery shortQuery = query.property(simpleShort);
        PropertyQuery intQuery = query.property(simpleInt);
        PropertyQuery longQuery = query.property(simpleLong);
        PropertyQuery floatQuery = query.property(simpleFloat);
        PropertyQuery doubleQuery = query.property(simpleDouble);
        PropertyQuery shortUQuery = query.property(simpleShortU);
        PropertyQuery intUQuery = query.property(simpleIntU);
        PropertyQuery longUQuery = query.property(simpleLongU);
        // avg
        assertEquals(0.5, booleanQuery.avg(), 0.0001);
        assertEquals(-37.5, byteQuery.avg(), 0.0001);
        assertEquals(2100.5, shortQuery.avg(), 0.0001);
        assertEquals(2000.5, intQuery.avg(), 0.0001);
        assertEquals(3000.5, longQuery.avg(), 0.0001);
        assertEquals(400.05, floatQuery.avg(), 0.0001);
        assertEquals(2020.005, doubleQuery.avg(), 0.0001);
        assertEquals(2100.5, shortUQuery.avg(), 0.0001);
        assertEquals(2000.5, intUQuery.avg(), 0.0001);
        assertEquals(3000.5, longUQuery.avg(), 0.0001);
        // avgLong
        assertEquals(1, booleanQuery.avgLong());
        assertEquals(-38, byteQuery.avgLong());
        assertEquals(2101, shortQuery.avgLong());
        assertEquals(2001, intQuery.avgLong());
        assertEquals(3001, longQuery.avgLong());
        assertEquals(2101, shortUQuery.avgLong());
        assertEquals(2001, intUQuery.avgLong());
        assertEquals(3001, longUQuery.avgLong());
        // min
        assertEquals(-38, byteQuery.min());
        assertEquals(2100, shortQuery.min());
        assertEquals(2000, intQuery.min());
        assertEquals(3000, longQuery.min());
        assertEquals(400, floatQuery.minDouble(), 0.001);
        assertEquals(2020, doubleQuery.minDouble(), 0.001);
        assertEquals(2100, shortUQuery.min());
        assertEquals(2000, intUQuery.min());
        assertEquals(3000, longUQuery.min());
        // max
        assertEquals(-37, byteQuery.max());
        assertEquals(2101, shortQuery.max());
        assertEquals(2001, intQuery.max());
        assertEquals(3001, longQuery.max());
        assertEquals(400.1, floatQuery.maxDouble(), 0.001);
        assertEquals(2020.01, doubleQuery.maxDouble(), 0.001);
        assertEquals(2101, shortUQuery.max());
        assertEquals(2001, intUQuery.max());
        assertEquals(3001, longUQuery.max());
        // sum
        assertEquals(1, booleanQuery.sum());
        assertEquals(1, booleanQuery.sumDouble(), 0.001);
        assertEquals(-75, byteQuery.sum());
        assertEquals(-75, byteQuery.sumDouble(), 0.001);
        assertEquals(4201, shortQuery.sum());
        assertEquals(4201, shortQuery.sumDouble(), 0.001);
        assertEquals(4001, intQuery.sum());
        assertEquals(4001, intQuery.sumDouble(), 0.001);
        assertEquals(6001, longQuery.sum());
        assertEquals(6001, longQuery.sumDouble(), 0.001);
        assertEquals(800.1, floatQuery.sumDouble(), 0.001);
        assertEquals(4040.01, doubleQuery.sumDouble(), 0.001);
        assertEquals(4201, shortUQuery.sum());
        assertEquals(4201, shortUQuery.sumDouble(), 0.001);
        assertEquals(4001, intUQuery.sum());
        assertEquals(4001, intUQuery.sumDouble(), 0.001);
        assertEquals(6001, longUQuery.sum());
        assertEquals(6001, longUQuery.sumDouble(), 0.001);
    }

    @Test
    public void testSumDoubleOfFloats() {
        TestEntity entity = new TestEntity();
        entity.setSimpleFloat(0);
        TestEntity entity2 = new TestEntity();
        entity2.setSimpleFloat(-2.05f);
        box.put(entity, entity2);
        double sum = box.query().build().property(simpleFloat).sumDouble();
        assertEquals(-2.05, sum, 0.0001);
    }

}
