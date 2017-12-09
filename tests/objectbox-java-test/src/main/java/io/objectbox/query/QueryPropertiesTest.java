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
import io.objectbox.query.QueryBuilder.StringOrder;

import static io.objectbox.TestEntity_.simpleInt;
import static io.objectbox.TestEntity_.simpleString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class QueryPropertiesTest extends AbstractObjectBoxTest {

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

        String[] result = query.findStrings(simpleString);
        assertEquals(5, result.length);
        assertEquals("BAR", result[0]);
        assertEquals("banana", result[1]);
        assertEquals("bar", result[2]);
        assertEquals("banana milk shake", result[3]);
        assertEquals("banana", result[4]);

        result = query.findStringsUnique(simpleString);
        assertEquals(3, result.length);
        List<String> list = Arrays.asList(result);
        assertTrue(list.contains("BAR"));
        assertTrue(list.contains("banana"));
        assertTrue(list.contains("banana milk shake"));

        result = query.findStringsUnique(simpleString, StringOrder.CASE_SENSITIVE);
        assertEquals(4, result.length);
        list = Arrays.asList(result);
        assertTrue(list.contains("BAR"));
        assertTrue(list.contains("banana"));
        assertTrue(list.contains("bar"));
        assertTrue(list.contains("banana milk shake"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFindStrings_wrongPropertyType() {
        putTestEntitiesStrings();
        box.query().build().findStrings(simpleInt);
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
