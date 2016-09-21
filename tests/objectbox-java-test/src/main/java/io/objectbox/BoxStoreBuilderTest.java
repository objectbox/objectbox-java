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
        BoxStore.clearDefaultStore();
        builder = new BoxStoreBuilder(createTestModel(false)).directory(boxStoreDir);
    }

    @Test
    public void testDefaultStore() {
        BoxStore boxStore = builder.buildDefault();
        assertSame(boxStore, BoxStore.getDefault());
        assertSame(boxStore, BoxStore.getDefault());
        try {
            builder.buildDefault();
            fail("Should have thrown");
        } catch (IllegalStateException expected) {
            // OK
        }
    }

    @Test
    public void testClearDefaultStore() {
        builder.buildDefault();
        BoxStore.clearDefaultStore();
        try {
            BoxStore.getDefault();
            fail("Should have thrown");
        } catch (IllegalStateException expected) {
            // OK
        }
        BoxStore boxStore = builder.buildDefault();
        assertSame(boxStore, BoxStore.getDefault());
    }

    @Test(expected = IllegalStateException.class)
    public void testDefaultStoreNull() {
        BoxStore.getDefault();
    }

}
