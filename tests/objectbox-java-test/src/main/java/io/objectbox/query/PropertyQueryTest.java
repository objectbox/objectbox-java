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

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.objectbox.AbstractObjectBoxTest;
import io.objectbox.Box;
import io.objectbox.BoxStoreBuilder;
import io.objectbox.DebugFlags;
import io.objectbox.TestEntity;
import io.objectbox.TestEntityCursor;
import io.objectbox.exception.DbException;
import io.objectbox.query.QueryBuilder.StringOrder;

import static io.objectbox.TestEntity_.*;
import static org.junit.Assert.*;

public class PropertyQueryTest extends AbstractObjectBoxTest {

    private Box<TestEntity> box;

    @Override
    protected BoxStoreBuilder createBoxStoreBuilder(boolean withIndex) {
        return super.createBoxStoreBuilder(withIndex).debugFlags(DebugFlags.LOG_QUERY_PARAMETERS);
    }

    @Before
    public void setUpBox() {
        box = getTestEntityBox();
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
        assertNull(query.property(simpleLong).findFirstLong());
        assertNull(query.property(simpleLong).findUniqueLong());
        putTestEntities(5);
        long result = query.property(simpleLong).findFirstLong();
        assertEquals(1003, result);

        query = box.query().greater(simpleLong, 1004).build();
        assertEquals(1005, (long) query.property(simpleLong).distinct().findUniqueLong());
    }

    @Test(expected = DbException.class)
    public void testFindLong_uniqueFails() {
        putTestEntity(null, 1);
        putTestEntity(null, 1);
        box.query().build().property(simpleLong).findUniqueLong();
    }

    @Test
    public void testFindInt() {
        Query<TestEntity> query = box.query().greater(simpleLong, 1002).build();
        assertNull(query.property(simpleInt).findFirstInt());
        assertNull(query.property(simpleInt).findUniqueInt());
        putTestEntities(5);
        int result = query.property(simpleInt).findFirstInt();
        assertEquals(3, result);

        query = box.query().greater(simpleLong, 1004).build();
        assertEquals(5, (int) query.property(simpleInt).distinct().findUniqueInt());

        TestEntityCursor.INT_NULL_HACK = true;
        try {
            putTestEntity(null, 6);
        } finally {
            TestEntityCursor.INT_NULL_HACK = false;
        }
        query.setParameter(simpleLong, 1005);
        assertEquals(-99, (int) query.property(simpleInt).nullValue(-99).findUniqueInt());
    }

    @Test(expected = DbException.class)
    public void testFindInt_uniqueFails() {
        putTestEntity(null, 1);
        putTestEntity(null, 1);
        box.query().build().property(simpleInt).findUniqueInt();
    }

    // TODO add test for other types of single object find methods

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

    private List<TestEntity> putTestEntitiesScalars() {
        return putTestEntities(10, null, 2000);
    }

    private List<TestEntity> putTestEntitiesStrings() {
        List<TestEntity> entities = new ArrayList<>();
        entities.add(createTestEntity("banana", 1));
        entities.add(createTestEntity("apple", 2));
        entities.add(createTestEntity("bar", 3));
        entities.add(createTestEntity("banana milk shake", 4));
        entities.add(createTestEntity("foo bar", 5));
        box.put(entities);
        return entities;
    }

}
