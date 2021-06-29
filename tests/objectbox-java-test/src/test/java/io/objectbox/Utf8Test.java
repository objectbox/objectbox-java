package io.objectbox;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * The Java VM does not recognize the four-byte format of standard UTF-8;
 * it uses its own two-times-three-byte format instead. Test to ensure these
 * supplementary characters (code points above U+FFFF) are properly supported.
 */
public class Utf8Test extends AbstractObjectBoxTest {

    // U+1F600
    private static final String TEST_STRING = "😁";

    @Test
    public void putGetAndQuery_works() {
        // Put
        TestEntity put = putTestEntity(TEST_STRING, 1);
        putTestEntity("🚀", 2); // U+1F680
        assertEquals(2, getTestEntityBox().count());

        // Get
        TestEntity get = getTestEntityBox().get(put.getId());
        assertEquals(TEST_STRING, get.getSimpleString());

        // Query String with equals
        List<TestEntity> results = getTestEntityBox().query(
                TestEntity_.simpleString.equal(TEST_STRING)
        ).build().find();
        assertEquals(1, results.size());
        assertEquals(TEST_STRING, results.get(0).getSimpleString());

        // Query String array
        List<TestEntity> resultsArray = getTestEntityBox().query(
                TestEntity_.simpleStringArray.containsElement(TEST_STRING)
        ).build().find();
        assertEquals(1, resultsArray.size());
        assertEquals(TEST_STRING, resultsArray.get(0).getSimpleString());
    }
}
