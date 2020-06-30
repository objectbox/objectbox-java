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

public class FlexMapConverterTest {

    @Test
    public void keysString_valsString_works() {
        FlexMapConverter converter = new StringFlexMapConverter();
        Map<String, String> map = new HashMap<>();

        convertAndBackThenAssert(null, converter);

        convertAndBackThenAssert(map, converter);

        map.put("Hello", "Grüezi");
        map.put("💡", "Idea");
        convertAndBackThenAssert(map, converter);
    }

    @Test
    public void keysString_valsSupportedTypes_works() {
        FlexMapConverter converter = new StringFlexMapConverter();
        Map<String, Object> map = new HashMap<>();

        convertAndBackThenAssert(null, converter);

        convertAndBackThenAssert(map, converter);

        map.put("string", "Grüezi");
        map.put("boolean", true);
//        map.put("integer", 1);
        map.put("long", -2L);
//        map.put("float", 1.3f);
        map.put("double", -1.4d);
        convertAndBackThenAssert(map, converter);
    }

    // Note: can't use assertEquals(map, map) as byte[] does not implement equals(obj).
    @SuppressWarnings("unchecked")
    @Test
    public void keysString_valsByteArray_works() {
        FlexMapConverter converter = new StringFlexMapConverter();
        Map<String, byte[]> map = new HashMap<>();

        map.put("bytearr", new byte[]{1, 2, 3});
        Map<String, byte[]> actual = convertAndBack(map, converter);

        assertEquals(map.size(), actual.size());
        assertArrayEquals(map.get("bytearr"), actual.get("bytearr"));
    }

    @Test
    public void keysInteger_works() {
        FlexMapConverter converter = new IntegerFlexMapConverter();
        Map<Integer, String> map = new HashMap<>();

        convertAndBackThenAssert(null, converter);

        convertAndBackThenAssert(map, converter);

        map.put(-1, "Grüezi");
        map.put(1, "Idea");
        convertAndBackThenAssert(map, converter);
    }

    @Test
    public void keysLong_works() {
        FlexMapConverter converter = new LongFlexMapConverter();
        Map<Long, String> map = new HashMap<>();

        convertAndBackThenAssert(null, converter);

        convertAndBackThenAssert(map, converter);

        map.put(-1L, "Grüezi");
        map.put(1L, "Idea");
        convertAndBackThenAssert(map, converter);
    }

    @Test
    public void nestedMap_works() {
        FlexMapConverter converter = new StringFlexMapConverter();
        // Restriction: map keys must all have same type.
        Map<String, Map<String, String>> map = new HashMap<>();

        convertAndBackThenAssert(null, converter);

        convertAndBackThenAssert(map, converter);

        Map<String, String> embeddedMap1 = new HashMap<>();
        embeddedMap1.put("Hello1", "Grüezi1");
        embeddedMap1.put("💡1", "Idea1");
        map.put("Hello", embeddedMap1);
        Map<String, String> embeddedMap2 = new HashMap<>();
        embeddedMap2.put("Hello2", "Grüezi2");
        embeddedMap2.put("💡2", "Idea2");
        map.put("💡", embeddedMap2);
        convertAndBackThenAssert(map, converter);
    }

    @Test
    public void nestedList_works() {
        FlexMapConverter converter = new StringFlexMapConverter();
        Map<String, List<Object>> map = new HashMap<>();

        convertAndBackThenAssert(null, converter);

        convertAndBackThenAssert(map, converter);

        List<Object> embeddedList1 = new LinkedList<>();
        embeddedList1.add("Grüezi");
        embeddedList1.add(true);
//        embeddedList1.add(1);
        embeddedList1.add(-2L);
//        embeddedList1.add(1.3f);
        embeddedList1.add(-1.4d);
        map.put("Hello", embeddedList1);
        List<Object> embeddedList2 = new LinkedList<>();
        embeddedList2.add("Grüezi");
        embeddedList2.add(true);
//        embeddedList2.add(21);
        embeddedList2.add(-22L);
//        embeddedList2.add(2.3f);
        embeddedList2.add(-2.4d);
        map.put("💡", embeddedList2);
        convertAndBackThenAssert(map, converter);
    }

    // Note: can't use assertEquals(map, map) as byte[] does not implement equals(obj).
    @SuppressWarnings("unchecked")
    @Test
    public void nestedListByteArray_works() {
        FlexMapConverter converter = new StringFlexMapConverter();
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
    public void nullKeyOrValue_throws() {
        FlexMapConverter converter = new StringFlexMapConverter();
        Map<String, String> map = new HashMap<>();

        map.put("Hello", null);
        convertThenAssertThrows(map, converter);

        map.clear();

        map.put(null, "Idea");
        convertThenAssertThrows(map, converter);
    }

    @Test
    public void unsupportedValue_throws() {
        FlexMapConverter converter = new StringFlexMapConverter();
        Map<String, Object> map = new HashMap<>();

        map.put("Hello", Instant.now());
        convertThenAssertThrows(map, converter);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Map convertAndBack(@Nullable Map expected, FlexMapConverter converter) {
        byte[] converted = converter.convertToDatabaseValue(expected);

        return converter.convertToEntityProperty(converted);
    }

    @SuppressWarnings({"rawtypes"})
    private void convertAndBackThenAssert(@Nullable Map expected, FlexMapConverter converter) {
        assertEquals(expected, convertAndBack(expected, converter));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void convertThenAssertThrows(Map map, FlexMapConverter converter) {
        assertThrows(
                IllegalArgumentException.class,
                () -> converter.convertToDatabaseValue(map)
        );
    }
}
