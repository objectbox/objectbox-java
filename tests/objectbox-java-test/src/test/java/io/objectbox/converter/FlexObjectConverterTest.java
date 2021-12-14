package io.objectbox.converter;

import org.junit.Test;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
        // Java Long is returned as Integer if it fits, so expect Integer.
        Object restoredLong = convertAndBack(1L, converter);
        assertEquals((int) 1L, restoredLong);
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
        list.add(-2L);
        list.add(1.3f);
        list.add(-1.4d);
        List<Object> restoredList = convertAndBack(list, converter);
        // Java Float is returned as Double, so expect Double.
        list.set(3, (double) 1.3f);
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
