/*
 * Copyright 2020-2024 ObjectBox Ltd.
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

package io.objectbox.converter;

import org.junit.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;


import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Tests {@link FlexObjectConverter} and subclasses with flexible maps.
 * For basic tests see {@link FlexObjectConverterTest}.
 */
public class FlexMapConverterTest {

    @Test
    public void keysString_valsSupportedTypes_works() {
        FlexObjectConverter converter = new StringFlexMapConverter();
        Map<String, Object> map = new HashMap<>();

        convertAndBackThenAssert(null, converter);

        convertAndBackThenAssert(map, converter);

        map.put("string", "Grüezi");
        map.put("boolean", true);
        map.put("byte", (byte) 1);
        map.put("short", (short) 1);
        map.put("int", 1);
        map.put("long", 1L);
        map.put("float", 1.3f);
        map.put("double", -1.4d);
        map.put("null", null);
        Map<String, Object> restoredMap = convertAndBack(map, converter);
        // Java integers are returned as Long if one value is larger than 32 bits, so expect Long.
        map.put("byte", 1L);
        map.put("short", 1L);
        map.put("int", 1L);
        // Java float is returned as double, so expect double.
        map.put("float", (double) 1.3f);
        assertEquals(map, restoredMap);
    }

    /**
     * If no item is wider than 32 bits, all integers are restored as Integer.
     */
    @Test
    public void keysString_valsIntegersBiggest32Bit_works() {
        FlexObjectConverter converter = new StringFlexMapConverter();
        Map<String, Object> expected = new HashMap<>();

        expected.put("integer-8bit", -1);
        expected.put("integer-32bit", Integer.MAX_VALUE);
        expected.put("long-8bit", -2L);
        expected.put("long-32bit", (long) Integer.MIN_VALUE);

        Map<String, Object> actual = convertAndBack(expected, converter);
        
        assertEquals(expected.size(), actual.size());
        for (Object value : actual.values()) {
            assertTrue(value instanceof Integer);
        }
    }

    /**
     * Using Long value converter, even if no item is wider than 32 bits, all integers are restored as Long.
     */
    @Test
    public void keysString_valsLongBiggest32Bit_works() {
        FlexObjectConverter converter = new StringLongMapConverter();
        Map<String, Long> expected = new HashMap<>();

        expected.put("long-8bit-neg", -1L);
        expected.put("long-8bit", 2L);
        expected.put("long-32bit-neg", (long) Integer.MIN_VALUE);
        expected.put("long-32bit", (long) Integer.MAX_VALUE);

        convertAndBackThenAssert(expected, converter);
    }

    /**
     * If at least one item is 64 bit wide, all integers are restored as Long.
     */
    @Test
    public void keysString_valsIntegersBiggest64Bit_works() {
        FlexObjectConverter converter = new StringFlexMapConverter();
        Map<String, Object> expected = new HashMap<>();

        expected.put("integer-8bit", -1);
        expected.put("integer-32bit", Integer.MAX_VALUE);
        expected.put("long-64bit", Integer.MAX_VALUE + 1L);

        Map<String, Object> actual = convertAndBack(expected, converter);

        assertEquals(expected.size(), actual.size());
        for (Object value : actual.values()) {
            assertTrue(value instanceof Long);
        }
    }

    // Note: can't use assertEquals(map, map) as byte[] does not implement equals(obj).
    @Test
    public void keysString_valsByteArray_works() {
        FlexObjectConverter converter = new StringFlexMapConverter();
        Map<String, byte[]> map = new HashMap<>();

        map.put("bytearr", new byte[]{1, 2, 3});
        Map<String, byte[]> actual = convertAndBack(map, converter);

        assertEquals(map.size(), actual.size());
        assertArrayEquals(map.get("bytearr"), actual.get("bytearr"));
    }

    @Test
    public void keysInteger_works() {
        FlexObjectConverter converter = new IntegerFlexMapConverter();
        Map<Integer, String> map = new HashMap<>();

        convertAndBackThenAssert(null, converter);

        convertAndBackThenAssert(map, converter);

        map.put(-1, "Grüezi");
        map.put(1, "Idea");
        convertAndBackThenAssert(map, converter);
    }

    @Test
    public void keysLong_works() {
        FlexObjectConverter converter = new LongFlexMapConverter();
        Map<Long, String> map = new HashMap<>();

        convertAndBackThenAssert(null, converter);

        convertAndBackThenAssert(map, converter);

        map.put(-1L, "Grüezi");
        map.put(1L, "Idea");
        convertAndBackThenAssert(map, converter);
    }

    @Test
    public void nestedMap_works() {
        FlexObjectConverter converter = new StringFlexMapConverter();
        // Restriction: map keys must all have same type.
        Map<String, Map<String, String>> map = new HashMap<>();

        convertAndBackThenAssert(null, converter);

        convertAndBackThenAssert(map, converter);

        Map<String, String> embeddedMap1 = new HashMap<>();
        embeddedMap1.put("Hello1", "Grüezi1");
        embeddedMap1.put("💡1", "Idea1");
        embeddedMap1.put("null1", null);
        map.put("Hello", embeddedMap1);
        Map<String, String> embeddedMap2 = new HashMap<>();
        embeddedMap2.put("Hello2", "Grüezi2");
        embeddedMap2.put("💡2", "Idea2");
        embeddedMap2.put("null2", null);
        map.put("💡", embeddedMap2);
        convertAndBackThenAssert(map, converter);
    }

    @Test
    public void nestedList_works() {
        FlexObjectConverter converter = new StringFlexMapConverter();
        Map<String, List<Object>> map = new HashMap<>();

        convertAndBackThenAssert(null, converter);

        convertAndBackThenAssert(map, converter);

        List<Object> embeddedList1 = new LinkedList<>();
        embeddedList1.add("Grüezi");
        embeddedList1.add(true);
        embeddedList1.add(-2L);
        embeddedList1.add(1.3f);
        embeddedList1.add(-1.4d);
        embeddedList1.add(null);
        map.put("Hello", embeddedList1);
        List<Object> embeddedList2 = new LinkedList<>();
        embeddedList2.add("Grüezi");
        embeddedList2.add(true);
        embeddedList2.add(-22L);
        embeddedList2.add(2.3f);
        embeddedList2.add(-2.4d);
        map.put("💡", embeddedList2);
        Map<String, List<Object>> restoredMap = convertAndBack(map, converter);
        // Java float is returned as double, so expect double.
        embeddedList1.set(3, (double) 1.3f);
        embeddedList2.set(3, (double) 2.3f);
        assertEquals(map, restoredMap);
    }

    // Note: can't use assertEquals(map, map) as byte[] does not implement equals(obj).
    @Test
    public void nestedListByteArray_works() {
        FlexObjectConverter converter = new StringFlexMapConverter();
        Map<String, List<byte[]>> map = new HashMap<>();

        List<byte[]> embeddedList = new LinkedList<>();
        embeddedList.add(new byte[]{1, 2, 3});
        map.put("list", embeddedList);
        Map<String, List<byte[]>> actual = convertAndBack(map, converter);

        assertEquals(map.size(), actual.size());
        assertEquals(map.get("list").size(), actual.get("list").size());
        assertArrayEquals(map.get("list").get(0), actual.get("list").get(0));
    }

    @Test
    public void nullKey_throws() {
        FlexObjectConverter converter = new StringFlexMapConverter();
        Map<String, String> map = new HashMap<>();

        map.put(null, "Idea");
        convertThenAssertThrows(map, converter, "Map keys must not be null");
    }

    @Test
    public void unsupportedKey_throws() {
        Map<Object, Object> map = new HashMap<>();
        map.put(false, "supported");

        convertThenAssertThrows(map, new FlexObjectConverter(), "Map keys must be String");
        convertThenAssertThrows(map, new StringLongMapConverter(), "Map keys must be String");
        convertThenAssertThrows(map, new IntegerFlexMapConverter(), "Map keys must be Integer");
        convertThenAssertThrows(map, new IntegerLongMapConverter(), "Map keys must be Integer");
        convertThenAssertThrows(map, new LongFlexMapConverter(), "Map keys must be Long");
        convertThenAssertThrows(map, new LongLongMapConverter(), "Map keys must be Long");
    }

    @Test
    public void unsupportedValue_throws() {
        FlexObjectConverter converter = new StringFlexMapConverter();
        Map<String, Object> map = new HashMap<>();

        map.put("Hello", Instant.now());
        convertThenAssertThrows(map, converter, "Map values of this type are not supported: Instant");
    }

    @SuppressWarnings("unchecked")
    private <K, V> Map<K, V> convertAndBack(@Nullable Map<K, V> expected, FlexObjectConverter converter) {
        byte[] converted = converter.convertToDatabaseValue(expected);

        return (Map<K, V>) converter.convertToEntityProperty(converted);
    }

    private <K, V> void convertAndBackThenAssert(@Nullable Map<K, V> expected, FlexObjectConverter converter) {
        assertEquals(expected, convertAndBack(expected, converter));
    }

    @SuppressWarnings({"rawtypes"})
    private void convertThenAssertThrows(Map map, FlexObjectConverter converter, String expectedMessage) {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> converter.convertToDatabaseValue(map)
        );
        assertEquals(expectedMessage, exception.getMessage());
    }
}
