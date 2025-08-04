/*
 * Copyright 2025 ObjectBox Ltd.
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

import io.objectbox.TestEntity;
import io.objectbox.TestEntity_;
import org.junit.Test;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.objectbox.TestEntity_.stringObjectMap;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class FlexQueryTest extends AbstractQueryTest {

    private TestEntity createFlexPropertyEntity(@Nullable Object flex) {
        TestEntity entity = new TestEntity();
        entity.setFlexProperty(flex);
        return entity;
    }

    /**
     * equals works for flexible string and integer properties.
     */
    @Test
    public void equals_flexString() {
        box.put(
                createFlexPropertyEntity("banana"),
                createFlexPropertyEntity(-1),
                createFlexPropertyEntity(1.3f),
                createFlexPropertyEntity(-1.4d),
                createFlexPropertyEntity(null)
        );

        assertFlexPropertyEqualsMatch("banana");
        assertFlexPropertyEqualsMatch(-1);

        // Check isNull as well
        List<TestEntity> results = box.query(TestEntity_.flexProperty.isNull()).build().find();
        assertEquals(1, results.size());
        assertNull(results.get(0).getFlexProperty());
    }

    private void assertFlexPropertyEqualsMatch(Object value) {
        List<TestEntity> results = box.query(TestEntity_.flexProperty.equal(value.toString())).build().find();
        assertEquals(1, results.size());
        assertEquals(value, results.get(0).getFlexProperty());
    }

    /**
     * containsElement matches strings and integers of flexible list.
     */
    @Test
    public void containsElement_flexList() {
        List<Object> flexListMatch = new ArrayList<>();
        flexListMatch.add("banana");
        flexListMatch.add(12);
        List<Object> flexListNoMatch = new ArrayList<>();
        flexListNoMatch.add("banana milk shake");
        flexListNoMatch.add(1234);
        List<Object> flexListNotSupported = new ArrayList<>();
        flexListNotSupported.add(1.3f);
        flexListNotSupported.add(-1.4d);
        box.put(
                createFlexPropertyEntity(flexListMatch),
                createFlexPropertyEntity(flexListNoMatch),
                createFlexPropertyEntity(flexListNotSupported),
                createFlexPropertyEntity(null)
        );

        assertFlexListContainsElement("banana");
        assertFlexListContainsElement(12);
    }

    @SuppressWarnings("unchecked")
    private void assertFlexListContainsElement(Object value) {
        List<TestEntity> results = box.query(TestEntity_.flexProperty.containsElement(value.toString())).build().find();
        assertEquals(1, results.size());
        List<Object> list = (List<Object>) results.get(0).getFlexProperty();
        assertNotNull(list);
        assertTrue(list.contains("banana"));
    }

    private TestEntity createFlexMapEntity(String s, boolean b, long l, float f, double d) {
        TestEntity entity = new TestEntity();
        Map<String, Object> map = new HashMap<>();
        map.put(s + "-string", s);
        map.put(s + "-boolean", b);
        map.put(s + "-long", l);
        map.put(s + "-float", f);
        map.put(s + "-double", d);
        map.put(s + "-list", Arrays.asList(s, b, l, f, d));
        Map<String, Object> embeddedMap = new HashMap<>();
        embeddedMap.put("embedded-key", "embedded-value");
        map.put(s + "-map", embeddedMap);
        entity.setStringObjectMap(map);
        return entity;
    }

    @Test
    public void contains_stringObjectMap() {
        // Note: map keys and values can not be null, so no need to test. See FlexMapConverterTest.
        box.put(
                createFlexMapEntity("banana", true, -1L, 1.3f, -1.4d),
                createFlexMapEntity("banana milk shake", false, 1L, -1.3f, 1.4d)
        );

        // contains throws when used with flex property.
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> box.query(stringObjectMap.contains("banana-string")));
        assertEquals("Property type is neither a string nor array of strings: Flex", exception.getMessage());

        // containsElement only matches if key is equal.
        assertContainsKey("banana-string");
        assertContainsKey("banana-boolean");
        assertContainsKey("banana-long");
        assertContainsKey("banana-float");
        assertContainsKey("banana-double");
        assertContainsKey("banana-list");
        assertContainsKey("banana-map");

        // containsKeyValue only matches if key and value is equal.
        assertQueryCondition(stringObjectMap.equalKeyValue("banana-string", "banana", QueryBuilder.StringOrder.CASE_SENSITIVE), 1);
        assertQueryCondition(stringObjectMap.equalKeyValue("banana-long", -1L), 1);

        // setParameters works with strings and integers.
        Query<TestEntity> setParamQuery = box.query(
                stringObjectMap.equalKeyValue("", "", QueryBuilder.StringOrder.CASE_SENSITIVE).alias("contains")
        ).build();
        assertEquals(0, setParamQuery.find().size());

        setParamQuery.setParameters(stringObjectMap, "banana-string", "banana");
        List<TestEntity> setParamResults = setParamQuery.find();
        assertEquals(1, setParamResults.size());
        assertTrue(setParamResults.get(0).getStringObjectMap().containsKey("banana-string"));

        setParamQuery.setParameters("contains", "banana milk shake-string", "banana milk shake");
        setParamResults = setParamQuery.find();
        assertEquals(1, setParamResults.size());
        assertTrue(setParamResults.get(0).getStringObjectMap().containsKey("banana milk shake-string"));

        setParamQuery.close();
    }

    private void assertContainsKey(String key) {
        try (Query<TestEntity> query = box.query(
                stringObjectMap.containsElement(key)
        ).build()) {
            List<TestEntity> results = query.find();
            assertEquals(1, results.size());
            assertTrue(results.get(0).getStringObjectMap().containsKey(key));
        }
    }

    private TestEntity createObjectWithStringObjectMap(String s, long l, double d) {
        TestEntity entity = new TestEntity();
        Map<String, Object> map = new HashMap<>();
        map.put("key-string", s);
        map.put("key-long", l);
        map.put("key-double", d);
        entity.setStringObjectMap(map);
        return entity;
    }

    private List<TestEntity> createObjectsWithStringObjectMap() {
        return Arrays.asList(
                createObjectWithStringObjectMap("apple", -1L, -0.2d),
                createObjectWithStringObjectMap("Cherry", 3L, -1234.56d),
                createObjectWithStringObjectMap("Apple", 234234234L, 1234.56d),
                createObjectWithStringObjectMap("pineapple", -567L, 0.1d)
        );
    }

    @Test
    public void greaterKeyValue_stringObjectMap() {
        List<TestEntity> objects = createObjectsWithStringObjectMap();
        box.put(objects);
        long apple = objects.get(0).getId();
        long Cherry = objects.get(1).getId();
        long Apple = objects.get(2).getId();
        long pineapple = objects.get(3).getId();

        // Note: CASE_SENSITIVE orders like "Apple, Cherry, apple, pineapple"
        assertQueryCondition(stringObjectMap.greaterKeyValue("key-string", "Cherry",
                QueryBuilder.StringOrder.CASE_SENSITIVE), apple, pineapple);
        assertQueryCondition(stringObjectMap.greaterKeyValue("key-string", "Cherry",
                QueryBuilder.StringOrder.CASE_INSENSITIVE), pineapple);
        assertQueryCondition(stringObjectMap.greaterKeyValue("key-long", -2L), apple, Cherry, Apple);
        assertQueryCondition(stringObjectMap.greaterKeyValue("key-long", 234234234L));
        assertQueryCondition(stringObjectMap.greaterKeyValue("key-double", 0.0d), Apple, pineapple);
        assertQueryCondition(stringObjectMap.greaterKeyValue("key-double", 1234.56d));
    }

    @Test
    public void greaterEqualsKeyValue_stringObjectMap() {
        List<TestEntity> objects = createObjectsWithStringObjectMap();
        box.put(objects);
        long apple = objects.get(0).getId();
        long Cherry = objects.get(1).getId();
        long Apple = objects.get(2).getId();
        long pineapple = objects.get(3).getId();

        // Note: CASE_SENSITIVE orders like "Apple, Cherry, apple, pineapple"
        assertQueryCondition(stringObjectMap.greaterOrEqualKeyValue("key-string", "Cherry",
                QueryBuilder.StringOrder.CASE_SENSITIVE), apple, Cherry, pineapple);
        assertQueryCondition(stringObjectMap.greaterOrEqualKeyValue("key-string", "Cherry",
                QueryBuilder.StringOrder.CASE_INSENSITIVE), Cherry, pineapple);
        assertQueryCondition(stringObjectMap.greaterOrEqualKeyValue("key-long", -2L), apple, Cherry, Apple);
        assertQueryCondition(stringObjectMap.greaterOrEqualKeyValue("key-long", 234234234L), Apple);
        assertQueryCondition(stringObjectMap.greaterOrEqualKeyValue("key-double", 0.05d), Apple, pineapple);
        assertQueryCondition(stringObjectMap.greaterOrEqualKeyValue("key-double", 1234.54d), Apple);
    }

    @Test
    public void lessKeyValue_stringObjectMap() {
        List<TestEntity> objects = createObjectsWithStringObjectMap();
        box.put(objects);
        long apple = objects.get(0).getId();
        long Cherry = objects.get(1).getId();
        long Apple = objects.get(2).getId();
        long pineapple = objects.get(3).getId();

        // Note: CASE_SENSITIVE orders like "Apple, Cherry, apple, pineapple"
        assertQueryCondition(stringObjectMap.lessKeyValue("key-string", "Cherry",
                QueryBuilder.StringOrder.CASE_SENSITIVE), Apple);
        assertQueryCondition(stringObjectMap.lessKeyValue("key-string", "Cherry",
                QueryBuilder.StringOrder.CASE_INSENSITIVE), apple, Apple);
        assertQueryCondition(stringObjectMap.lessKeyValue("key-long", -2L), pineapple);
        assertQueryCondition(stringObjectMap.lessKeyValue("key-long", 6734234234L), apple, Cherry, Apple, pineapple);
        assertQueryCondition(stringObjectMap.lessKeyValue("key-double", 0.0d), apple, Cherry);
        assertQueryCondition(stringObjectMap.lessKeyValue("key-double", 1234.56d), apple, Cherry, pineapple);
    }

    @Test
    public void lessEqualsKeyValue_stringObjectMap() {
        List<TestEntity> objects = createObjectsWithStringObjectMap();
        box.put(objects);
        long apple = objects.get(0).getId();
        long Cherry = objects.get(1).getId();
        long Apple = objects.get(2).getId();
        long pineapple = objects.get(3).getId();

        // Note: CASE_SENSITIVE orders like "Apple, Cherry, apple, pineapple"
        assertQueryCondition(stringObjectMap.lessOrEqualKeyValue("key-string", "Cherry",
                QueryBuilder.StringOrder.CASE_SENSITIVE), Cherry, Apple);
        assertQueryCondition(stringObjectMap.lessOrEqualKeyValue("key-string", "Cherry",
                QueryBuilder.StringOrder.CASE_INSENSITIVE), apple, Cherry, Apple);
        assertQueryCondition(stringObjectMap.lessOrEqualKeyValue("key-long", -1L), apple, pineapple);
        assertQueryCondition(stringObjectMap.lessOrEqualKeyValue("key-long", -567L), pineapple);
        assertQueryCondition(stringObjectMap.lessOrEqualKeyValue("key-double", 0.0d), apple, Cherry);
        assertQueryCondition(stringObjectMap.lessOrEqualKeyValue("key-double", 1234.56d), apple, Cherry, Apple, pineapple);
    }

    private void assertQueryCondition(PropertyQueryCondition<TestEntity> condition, long... expectedIds) {
        try (Query<TestEntity> query = box.query(condition).build()) {
            List<TestEntity> results = query.find();
            assertResultIds(expectedIds, results);
        }
    }

    private void assertResultIds(long[] expected, List<TestEntity> results) {
        assertArrayEquals(expected, results.stream().mapToLong(TestEntity::getId).toArray());
    }

}
