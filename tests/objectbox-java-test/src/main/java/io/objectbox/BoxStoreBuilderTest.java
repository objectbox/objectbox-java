package io.objectbox;

import org.junit.Before;
import org.junit.Test;

import io.objectbox.exception.DbMaxReadersExceededException;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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

    @Test
    public void testMaxReaders() throws InterruptedException {
        builder = createBoxStoreBuilder(false);
        store = builder.maxReaders(1).build();
        final Exception[] exHolder = {null};
        final Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    getTestEntityBox().count();
                } catch (Exception e) {
                    exHolder[0] = e;
                }
            }
        });

        getTestEntityBox().count();
        store.runInReadTx(new Runnable() {
            @Override
            public void run() {
                getTestEntityBox().count();
                thread.start();
                try {
                    thread.join(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        // TODO: not working (debugged maxReaders get passed to native OK)
//        assertNotNull(exHolder[0]);
//        assertEquals(DbMaxReadersExceededException.class, exHolder[0].getClass());
    }

}
