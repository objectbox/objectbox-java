package io.objectbox.converter;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;


import static org.junit.Assert.assertEquals;

public class StringMapConverterTest {

    @Test
    public void works() {
        convertAndBackThenAssert(null);

        convertAndBackThenAssert(new HashMap<>());

        Map<String, String> mapWithValues = new HashMap<>();
        mapWithValues.put("Hello", "Grüezi");
        mapWithValues.put("💡", "Idea");
        mapWithValues.put("", "Empty String Key");
        convertAndBackThenAssert(mapWithValues);
    }

    private void convertAndBackThenAssert(@Nullable Map<String, String> expected) {
        StringMapConverter converter = new StringMapConverter();
        byte[] converted = converter.convertToDatabaseValue(expected);

        Map<String, String> actual = converter.convertToEntityProperty(converted);
        assertEquals(expected, actual);
    }
}
