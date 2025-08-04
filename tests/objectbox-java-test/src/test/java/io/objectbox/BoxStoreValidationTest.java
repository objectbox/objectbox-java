/*
 * Copyright 2023-2024 ObjectBox Ltd.
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

import org.greenrobot.essentials.io.IoUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import io.objectbox.config.ValidateOnOpenModePages;
import io.objectbox.exception.FileCorruptException;
import io.objectbox.exception.PagesCorruptException;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeFalse;

/**
 * Tests validation (and recovery) options on opening a store.
 */
public class BoxStoreValidationTest extends AbstractObjectBoxTest {

    private BoxStoreBuilder builder;

    @Override
    protected BoxStore createBoxStore() {
        // Standard setup of store not required
        return null;
    }

    @Before
    public void setUpBuilder() {
        BoxStore.clearDefaultStore();
        builder = createBuilderWithTestModel().directory(boxStoreDir);
    }

    @Test
    public void validateOnOpen() {
        // Create a database first; we must create the model only once (ID/UID sequences would be different 2nd time)
        byte[] model = createTestModel(null);
        long id = buildNotCorruptedDatabase(model);

        // Then re-open database with validation and ensure db is operational
        builder = new BoxStoreBuilder(model).directory(boxStoreDir);
        builder.entity(new TestEntity_());
        builder.validateOnOpen(ValidateOnOpenModePages.Full);
        store = builder.build();
        assertNotNull(getTestEntityBox().get(id));
        getTestEntityBox().put(new TestEntity(0));
    }


    @Test
    public void validateOnOpenCorruptFile() throws IOException {
        assumeFalse(IN_MEMORY);

        File dir = prepareTempDir("object-store-test-corrupted");
        prepareBadDataFile(dir, "corrupt-pageno-in-branch-data.mdb");

        builder = BoxStoreBuilder.createDebugWithoutModel().directory(dir);
        builder.validateOnOpen(ValidateOnOpenModePages.Full);

        @SuppressWarnings("resource")
        FileCorruptException ex = assertThrows(PagesCorruptException.class, () -> builder.build());
        assertEquals("Validating pages failed (page not found)", ex.getMessage());

        // Clean up
        cleanUpAllFiles(dir);
    }

    @Test
    public void usePreviousCommitWithCorruptFile() throws IOException {
        assumeFalse(IN_MEMORY);

        File dir = prepareTempDir("object-store-test-corrupted");
        prepareBadDataFile(dir, "corrupt-pageno-in-branch-data.mdb");
        builder = BoxStoreBuilder.createDebugWithoutModel().directory(dir);
        builder.validateOnOpen(ValidateOnOpenModePages.Full).usePreviousCommit();
        store = builder.build();
        String diagnoseString = store.diagnose();
        assertTrue(diagnoseString.contains("entries=2"));
        store.validate(0, true);
        store.close();
        assertTrue(store.deleteAllFiles());
    }

    @Test
    public void usePreviousCommitAfterFileCorruptException() throws IOException {
        assumeFalse(IN_MEMORY);

        File dir = prepareTempDir("object-store-test-corrupted");
        prepareBadDataFile(dir, "corrupt-pageno-in-branch-data.mdb");
        builder = BoxStoreBuilder.createDebugWithoutModel().directory(dir);
        builder.validateOnOpen(ValidateOnOpenModePages.Full);
        try {
            store = builder.build();
            fail("Should have thrown");
        } catch (PagesCorruptException e) {
            builder.usePreviousCommit();
            store = builder.build();
        }

        String diagnoseString = store.diagnose();
        assertTrue(diagnoseString.contains("entries=2"));
        store.validate(0, true);
        store.close();
        assertTrue(store.deleteAllFiles());
    }

    @Test
    public void validateOnOpenKv() {
        // Create a database first; we must create the model only once (ID/UID sequences would be different 2nd time)
        byte[] model = createTestModel(null);
        long id = buildNotCorruptedDatabase(model);

        // Then re-open database with validation and ensure db is operational
        builder = new BoxStoreBuilder(model).directory(boxStoreDir);
        builder.entity(new TestEntity_());
        builder.validateOnOpenKv();
        store = builder.build();
        assertNotNull(getTestEntityBox().get(id));
        getTestEntityBox().put(new TestEntity(0));
    }

    @Test
    public void validateOnOpenKvCorruptFile() throws IOException {
        assumeFalse(IN_MEMORY);
        
        File dir = prepareTempDir("obx-store-validate-kv-corrupted");
        prepareBadDataFile(dir, "corrupt-keysize0-data.mdb");

        builder = BoxStoreBuilder.createDebugWithoutModel().directory(dir);
        builder.validateOnOpenKv();

        @SuppressWarnings("resource")
        FileCorruptException ex = assertThrows(FileCorruptException.class, () -> builder.build());
        assertEquals("KV validation failed; key is empty (KV pair number: 1, key size: 0, data size: 112)",
                ex.getMessage());

        // Clean up
        cleanUpAllFiles(dir);
    }

    /**
     * Returns the id of the inserted test entity.
     */
    private long buildNotCorruptedDatabase(byte[] model) {
        builder = new BoxStoreBuilder(model).directory(boxStoreDir);
        builder.entity(new TestEntity_());
        store = builder.build();

        TestEntity object = new TestEntity(0);
        object.setSimpleString("hello hello");
        long id = getTestEntityBox().put(object);
        store.close();
        return id;
    }

    /**
     * Copies the given file from resources to the given directory as "data.mdb".
     */
    private void prepareBadDataFile(File dir, String resourceName) throws IOException {
        assertTrue(dir.mkdir());
        File badDataFile = new File(dir, "data.mdb");
        try (InputStream badIn = getClass().getResourceAsStream(resourceName)) {
            assertNotNull(badIn);
            try (FileOutputStream badOut = new FileOutputStream(badDataFile)) {
                IoUtils.copyAllBytes(badIn, badOut);
            }
        }
    }

}
