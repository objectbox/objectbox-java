package io.objectbox;

import org.junit.After;
import org.junit.Before;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import io.objectbox.ModelBuilder.EntityBuilder;
import io.objectbox.model.PropertyFlags;
import io.objectbox.model.PropertyType;

public abstract class AbstractObjectBoxTest {
    protected File boxStoreDir;
    protected BoxStore store;
    protected Random random = new Random();
    protected boolean runExtensiveTests;
    int lastEntityId;
    long lastRefId;

    @Before
    public void setUp() throws IOException {
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

    protected BoxStoreBuilder createBoxStoreBuilderWithTwoEntities(boolean withIndex) {
        BoxStoreBuilder builder = new BoxStoreBuilder(createTestModelWithTwoEntities(withIndex)).directory(boxStoreDir);
        builder.entity("TestEntity", TestEntity.class, TestEntityCursor.class, new TestEntityProperties());
        builder.entity("TestEntityMinimal", TestEntityMinimal.class, TestEntityMinimalCursor.class, new Properties() {
            @Override
            public Property[] getAllProperties() {
                return new Property[0];
            }

            @Override
            public Property getIdProperty() {
                return null;
            }

            @Override
            public String getDbName() {
                return null;
            }
        });
        return builder;
    }

    protected BoxStoreBuilder createBoxStoreBuilder(boolean withIndex) {
        BoxStoreBuilder builder = new BoxStoreBuilder(createTestModel(withIndex)).directory(boxStoreDir);
        builder.entity("TestEntity", TestEntity.class, TestEntityCursor.class, new TestEntityProperties());
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
        addTestEntity(modelBuilder, withIndex);
        return modelBuilder.build();
    }

    byte[] createTestModelWithTwoEntities(boolean withIndex) {
        ModelBuilder modelBuilder = new ModelBuilder();
        addTestEntity(modelBuilder, withIndex);
        addTestEntityMinimal(modelBuilder, withIndex);
        return modelBuilder.build();
    }

    private void addTestEntity(ModelBuilder modelBuilder, boolean withIndex) {
        EntityBuilder entityBuilder = modelBuilder.entity("TestEntity", ++lastEntityId, ++lastRefId);
        entityBuilder.property("id", PropertyType.Long).flags(PropertyFlags.ID);
        entityBuilder.property("simpleBoolean", PropertyType.Bool);
        entityBuilder.property("simpleByte", PropertyType.Byte);
        entityBuilder.property("simpleShort", PropertyType.Short);
        entityBuilder.property("simpleInt", PropertyType.Int);
        entityBuilder.property("simpleLong", PropertyType.Long);
        entityBuilder.property("simpleFloat", PropertyType.Float);
        entityBuilder.property("simpleDouble", PropertyType.Double);
        entityBuilder.property("simpleString", PropertyType.String).flags(withIndex ? PropertyFlags.INDEXED : 0);
        entityBuilder.property("simpleByteArray", PropertyType.ByteVector);
        entityBuilder.entityDone();
    }

    private void addTestEntityMinimal(ModelBuilder modelBuilder, boolean withIndex) {
        EntityBuilder entityBuilder = modelBuilder.entity("TestEntityMinimal", ++lastEntityId, ++lastRefId);
        entityBuilder.property("id", PropertyType.Long).flags(PropertyFlags.ID);
        entityBuilder.property("text", PropertyType.String).flags(withIndex ? PropertyFlags.INDEXED : 0);
        entityBuilder.entityDone();
    }

}