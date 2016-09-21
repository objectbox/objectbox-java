package io.objectbox;

import org.junit.Before;
import org.junit.Test;


import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

public class BoxStoreBuilderTest extends AbstractObjectBoxTest {

    private BoxStoreBuilder builder;

    @Override
    protected BoxStore createBoxStore() {
        // Standard setup of store not required
        return null;
    }

    @Before
    public void setUpBox() {
        BoxStoreBuilder.clearDefaultStore();
        builder = new BoxStoreBuilder(createTestModel(false)).directory(boxStoreDir);
    }

    @Test
    public void testDefaultStore() {
        BoxStore boxStore = builder.buildDefault();
        assertSame(boxStore, BoxStoreBuilder.defaultStore());
        assertSame(boxStore, BoxStoreBuilder.defaultStore());
        try {
            builder.buildDefault();
            fail("Should have thrown");
        } catch (IllegalStateException expected) {
            // OK
        }
    }

    @Test
    public void testClearDefaultStore() {
        BoxStore boxStore = builder.buildDefault();
        BoxStoreBuilder.clearDefaultStore();
        try {
            BoxStoreBuilder.defaultStore();
            fail("Should have thrown");
        } catch (IllegalStateException expected) {
            // OK
        }
        boxStore = builder.buildDefault();
        assertSame(boxStore, BoxStoreBuilder.defaultStore());
    }

    @Test(expected = IllegalStateException.class)
    public void testDefaultStoreNull() {
        BoxStoreBuilder.defaultStore();
    }

}
