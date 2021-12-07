package io.objectbox.query;

import io.objectbox.TestEntity;
import io.objectbox.TestEntity_;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class FlexQueryTest extends AbstractQueryTest {

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
        assertContainsKeyValue("banana-long", -1L);
        // containsKeyValue only supports strings and integers.

        // setParameters works with strings and integers.
        Query<TestEntity> setParamQuery = box.query(
                TestEntity_.stringObjectMap.containsKeyValue("", "").alias("contains")
        ).build();
        assertEquals(0, setParamQuery.find().size());
        
        setParamQuery.setParameters(TestEntity_.stringObjectMap, "banana-string", "banana");
        List<TestEntity> setParamResults = setParamQuery.find();
        assertEquals(1, setParamResults.size());
        assertTrue(setParamResults.get(0).getStringObjectMap().containsKey("banana-string"));

        setParamQuery.setParameters("contains", "banana milk shake-long", Long.toString(1));
        setParamResults = setParamQuery.find();
        assertEquals(1, setParamResults.size());
        assertTrue(setParamResults.get(0).getStringObjectMap().containsKey("banana milk shake-long"));
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
