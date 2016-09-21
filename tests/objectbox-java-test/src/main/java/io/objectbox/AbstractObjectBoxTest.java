package io.objectbox;

import org.junit.After;
import org.junit.Before;

import java.io.File;
import java.util.Random;

import io.objectbox.ModelBuilder.EntityBuilder;
import io.objectbox.model.PropertyFlags;
import io.objectbox.model.PropertyType;

public abstract class AbstractObjectBoxTest {
    protected File boxStoreDir;
    protected BoxStore store;
    protected Random random = new Random();
    protected boolean runExtensiveTests;

    @Before
    public void setUp() throws Exception {
        // This works with Android without needing any context
        File tempFile = File.createTempFile("object-store-test", "");
        tempFile.delete();
        boxStoreDir = tempFile;
        store = createBoxStore();
        runExtensiveTests = System.getProperty("extensive") != null;
    }

    protected BoxStore createBoxStore() {
        return createBoxStore(false);
    }

    protected BoxStore createBoxStore(boolean withIndex) {
        return createBoxStoreBuilder(withIndex).build();
    }

    protected BoxStoreBuilder createBoxStoreBuilder(boolean withIndex) {
        BoxStoreBuilder builder = new BoxStoreBuilder(createTestModel(withIndex)).directory(boxStoreDir);
        builder.entity("TestEntity", TestEntity.class, TestEntityCursor.class);
        return builder;
    }

    protected Box<TestEntity> getTestEntityBox() {
        return store.boxFor(TestEntity.class);
    }

    @After
    public void tearDown() throws Exception {
        if (store != null) {
            try {
                store.close();
                store.deleteAllFiles();

                File[] files = boxStoreDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        logError("File was not deleted: " + file.getAbsolutePath());
                    }
                }
            } catch (Exception e) {
                logError("Could not clean up test", e);
            }
        }
        deleteAllFiles();
    }

    protected void deleteAllFiles() {
        if (boxStoreDir != null && boxStoreDir.exists()) {
            File[] files = boxStoreDir.listFiles();
            for (File file : files) {
                delete(file);
            }
            delete(boxStoreDir);
        }
    }

    private boolean delete(File file) {
        boolean deleted = file.delete();
        if (!deleted) {
            file.deleteOnExit();
            logError("Could not delete " + file.getAbsolutePath());
        }
        return deleted;
    }

    protected void log(String text) {
        System.out.println(text);
    }

    protected void logError(String text) {
        System.err.println(text);
    }

    protected void logError(String text, Exception ex) {
        if (text != null) {
            System.err.println(text);
        }
        ex.printStackTrace();
    }

    protected long time() {
        return System.currentTimeMillis();
    }

    byte[] createTestModel(boolean withIndex) {
        ModelBuilder modelBuilder = new ModelBuilder();

        EntityBuilder entityBuilder = modelBuilder.entity("TestEntity");
        entityBuilder.property("id", PropertyType.Long, PropertyFlags.ID);
        entityBuilder.property("simpleBoolean", PropertyType.Bool, 0);
        entityBuilder.property("simpleByte", PropertyType.Byte, 0);
        entityBuilder.property("simpleShort", PropertyType.Short, 0);
        entityBuilder.property("simpleInt", PropertyType.Int, 0);
        entityBuilder.property("simpleLong", PropertyType.Long, 0);
        entityBuilder.property("simpleFloat", PropertyType.Float, 0);
        entityBuilder.property("simpleDouble", PropertyType.Double, 0);
        entityBuilder.property("simpleString", PropertyType.String, withIndex ? PropertyFlags.INDEXED : 0);
        entityBuilder.property("simpleByteArray", PropertyType.ByteVector, 0);
        entityBuilder.entityDone();

        return modelBuilder.build();
    }

}