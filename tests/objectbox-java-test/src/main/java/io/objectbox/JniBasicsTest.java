package io.objectbox;

import org.junit.Before;
import org.junit.Test;

import io.objectbox.internal.JniTest;


import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class JniBasicsTest {
    @Before
    public void loadLib() {
        BoxStore.getVersionNative();
    }

    @Test
    public void testReturnIntArray() {
        // Lower Android versions have a ReferenceTable with 1024 entries only
        for (int i = 0; i < 2000; i++) {
            int[] ints = JniTest.returnIntArray();
            assertNotNull(ints);
        }
    }

    @Test
    public void testCreateAndDeleteIntArray() {
        // Lower Android versions have a ReferenceTable with 1024 entries only
        for (int i = 0; i < 2000; i++) {
            assertTrue(JniTest.createAndDeleteIntArray());
            System.out.print(i);
        }
    }
}
