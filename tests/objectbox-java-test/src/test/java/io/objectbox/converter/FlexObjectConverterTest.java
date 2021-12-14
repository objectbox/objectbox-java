package io.objectbox.converter;

import org.junit.Test;

import javax.annotation.Nullable;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

/**
 * Tests {@link FlexObjectConverter} basic types and flexible list conversion.
 * Flexible maps conversion is tested by {@link FlexMapConverterTest}.
 */
public class FlexObjectConverterTest {

    @Test
    public void supportedBasicTypes_works() {
        FlexObjectConverter converter = new FlexObjectConverter();

        convertAndBackThenAssert(null, converter);

        convertAndBackThenAssert("Grüezi", converter);
        convertAndBackThenAssert(true, converter);
        // Java integers are returned as Integer if no value is larger than 32 bits, so expect Integer.
        Object restoredByte = convertAndBack((byte) 1, converter);
        assertEquals(1, restoredByte);
        Object restoredShort = convertAndBack((short) 1, converter);
        assertEquals(1, restoredShort);
        Object restoredInteger = convertAndBack(1, converter);
        assertEquals(1, restoredInteger);
        Object restoredLong = convertAndBack(1L, converter);
        assertEquals(1, restoredLong);
        // Java Float is returned as Double, so expect Double.
        Object restoredFloat = convertAndBack(1.3f, converter);
        assertEquals((double) 1.3f, restoredFloat);
        convertAndBackThenAssert(1.4d, converter);
    }

    @Test
    public void list_works() {
        FlexObjectConverter converter = new FlexObjectConverter();
        List<Object> list = new LinkedList<>();

        // empty list
        convertAndBackThenAssert(list, converter);

        // list with supported types
        list.add("Grüezi");
        list.add(true);
        list.add((byte) 1);
        list.add((short) 1);
        list.add(1);
        list.add(-2L);
        list.add(1.3f);
        list.add(-1.4d);
        List<Object> restoredList = convertAndBack(list, converter);
        // Java integers are returned as Long as one element is larger than 32 bits, so expect Long.
        list.set(2, 1L);
        list.set(3, 1L);
        list.set(4, 1L);
        // Java Float is returned as Double, so expect Double.
        list.set(6, (double) 1.3f);
        assertEquals(list, restoredList);

        // list with null element
        list.add(null);
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> convertAndBack(list, converter)
        );
        assertEquals("List elements must not be null", exception.getMessage());
    }

    @SuppressWarnings("unchecked")
    private <T> T convertAndBack(@Nullable T expected, FlexObjectConverter converter) {
        byte[] converted = converter.convertToDatabaseValue(expected);

        return (T) converter.convertToEntityProperty(converted);
    }

    private <T> void convertAndBackThenAssert(@Nullable T expected, FlexObjectConverter converter) {
        assertEquals(expected, convertAndBack(expected, converter));
    }

}
