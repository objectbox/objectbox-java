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
import io.objectbox.query.QueryBuilder.StringOrder;


import static io.objectbox.TestEntity_.simpleBoolean;
import static io.objectbox.TestEntity_.simpleByte;
import static io.objectbox.TestEntity_.simpleDouble;
import static io.objectbox.TestEntity_.simpleFloat;
import static io.objectbox.TestEntity_.simpleInt;
import static io.objectbox.TestEntity_.simpleLong;
import static io.objectbox.TestEntity_.simpleShort;
import static io.objectbox.TestEntity_.simpleString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class PropertyQueryTest extends AbstractQueryTest {

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
        putTestEntity(null, 1000);
        putTestEntity("BAR", 100);
        putTestEntitiesStrings();
        putTestEntity("banana", 101);
        Query<TestEntity> query = box.query().build();
        PropertyQuery stringQuery = query.property(simpleString);
        assertEquals(8, query.count());
        assertEquals(7, stringQuery.count());
        assertEquals(6, stringQuery.distinct().count());
    }

}
