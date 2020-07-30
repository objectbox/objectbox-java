/*
 * Copyright 2017-2020 ObjectBox Ltd. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.objectbox;

import io.objectbox.model.ValidateOnOpenMode;
import org.junit.Before;
import org.junit.Test;


import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertNotNull;

public class BoxStoreBuilderTest extends AbstractObjectBoxTest {

    private BoxStoreBuilder builder;

    @Override
    protected BoxStore createBoxStore() {
        // Standard setup of store not required
        return null;
    }

    @Before
    public void setUpBuilder() {
        BoxStore.clearDefaultStore();
        builder = new BoxStoreBuilder(createTestModel(false)).directory(boxStoreDir);
    }

    @Test
    public void testDefaultStore() {
        BoxStore boxStore = builder.buildDefault();
        assertSame(boxStore, BoxStore.getDefault());
        assertSame(boxStore, BoxStore.getDefault());
        boxStore.close(); // to prevent "Another BoxStore was opened" error
        try {
            builder.buildDefault();
            fail("Should have thrown");
        } catch (IllegalStateException expected) {
            // OK
        }
    }

    @Test
    public void testClearDefaultStore() {
        BoxStore boxStore1 = builder.buildDefault();
        BoxStore.clearDefaultStore();
        try {
            BoxStore.getDefault();
            fail("Should have thrown");
        } catch (IllegalStateException expected) {
            // OK
        }
        boxStore1.close();
        BoxStore boxStore = builder.buildDefault();
        assertSame(boxStore, BoxStore.getDefault());
    }

    @Test(expected = IllegalStateException.class)
    public void testDefaultStoreNull() {
        BoxStore.getDefault();
    }

    @Test
    public void testMaxReaders() {
        builder = createBoxStoreBuilder(false);
        store = builder.maxReaders(1).build();
        final Exception[] exHolder = {null};
        final Thread thread = new Thread(() -> {
            try {
                getTestEntityBox().count();
            } catch (Exception e) {
                exHolder[0] = e;
            }
            getTestEntityBox().closeThreadResources();
        });

        getTestEntityBox().count();
        store.runInReadTx(() -> {
            getTestEntityBox().count();
            thread.start();
            try {
                thread.join(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        // TODO: not working (debugged maxReaders get passed to native OK)
//        assertNotNull(exHolder[0]);
//        assertEquals(DbMaxReadersExceededException.class, exHolder[0].getClass());
    }

    @Test
    public void readOnly() {
        // Create a database first; we must create the model only once (ID/UID sequences would be different 2nd time)
        byte[] model = createTestModel(false);
        builder = new BoxStoreBuilder(model).directory(boxStoreDir);
        store = builder.build();
        store.close();

        // Then re-open database with same model as read-only.
        builder = new BoxStoreBuilder(model).directory(boxStoreDir);
        builder.readOnly();
        store = builder.build();

        assertTrue(store.isReadOnly());
    }

    @Test
    public void validateOnOpen() {
        // Create a database first; we must create the model only once (ID/UID sequences would be different 2nd time)
        byte[] model = createTestModel(false);
        builder = new BoxStoreBuilder(model).directory(boxStoreDir);
        builder.entity(new TestEntity_());
        store = builder.build();

        TestEntity object = new TestEntity(0);
        object.setSimpleString("hello hello");
        long id = getTestEntityBox().put(object);
        store.close();

        // Then re-open database with validation and ensure db is operational
        builder = new BoxStoreBuilder(model).directory(boxStoreDir);
        builder.entity(new TestEntity_());
        builder.validateOnOpen(ValidateOnOpenMode.Full);
        store = builder.build();
        assertNotNull(getTestEntityBox().get(id));
        getTestEntityBox().put(new TestEntity(0));
    }
}
