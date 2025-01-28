/*
 * Copyright 2025 ObjectBox Ltd. All rights reserved.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import io.objectbox.TestEntity;
import io.objectbox.TestEntity_;


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
                () -> box.query(TestEntity_.stringObjectMap.contains("banana-string")));
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
        assertContainsKeyValue("banana-string", "banana");
        // containsKeyValue only supports strings for now (TODO: until objectbox#1099 functionality is added).
        // assertContainsKeyValue("banana-long", -1L);

        // setParameters works with strings and integers.
        Query<TestEntity> setParamQuery = box.query(
                TestEntity_.stringObjectMap.containsKeyValue("", "").alias("contains")
        ).build();
        assertEquals(0, setParamQuery.find().size());

        setParamQuery.setParameters(TestEntity_.stringObjectMap, "banana-string", "banana");
        List<TestEntity> setParamResults = setParamQuery.find();
        assertEquals(1, setParamResults.size());
        assertTrue(setParamResults.get(0).getStringObjectMap().containsKey("banana-string"));

        setParamQuery.setParameters("contains", "banana milk shake-string", "banana milk shake");
        setParamResults = setParamQuery.find();
        assertEquals(1, setParamResults.size());
        assertTrue(setParamResults.get(0).getStringObjectMap().containsKey("banana milk shake-string"));
    }

    private void assertContainsKey(String key) {
        List<TestEntity> results = box.query(
                TestEntity_.stringObjectMap.containsElement(key)
        ).build().find();
        assertEquals(1, results.size());
        assertTrue(results.get(0).getStringObjectMap().containsKey(key));
    }

    private void assertContainsKeyValue(String key, Object value) {
        List<TestEntity> results = box.query(
                TestEntity_.stringObjectMap.containsKeyValue(key, value.toString())
        ).build().find();
        assertEquals(1, results.size());
        assertTrue(results.get(0).getStringObjectMap().containsKey(key));
        assertEquals(value, results.get(0).getStringObjectMap().get(key));
    }
}
