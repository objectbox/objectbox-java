package io.objectbox;

import org.junit.Test;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class PerformanceTest extends AbstractObjectBoxTest {

    @Override
    protected BoxStore createBoxStore() {
        // No default store because we do indexed & unindexed stuff here
        return null;
    }

    @Override
    protected BoxStore createBoxStore(boolean withIndex) {
        // We need more space
        BoxStore boxStore = createBoxStoreBuilder(withIndex).maxSizeInKByte(100 * 1024).build();
        // boxStore.dropAllData();
        return boxStore;
    }

    @Test
    public void testFindLongPerformance() {
        store = createBoxStore(false);
        testFindPerformance(100000, false, "without index");
    }

    @Test
    public void testFindStringPerformance() {
        store = createBoxStore(false);
        testFindPerformance(100000, true, "without index");
    }

    @Test
    public void testFindStringPerformanceWithIndex() {
        store = createBoxStore(true);
        testFindPerformance(100000, true, "with index");
    }

    private void testFindPerformance(int count, boolean findString, String withOrWithoutIndex) {
        TestEntity[] entities = bulkInsert(count, withOrWithoutIndex, "ObjectBox Foo Bar x ");
        findSingle(1, entities, findString);
        findSingle(count / 100, entities, findString);
        findSingle(count / 10, entities, findString);
        findSingle(count / 2, entities, findString);
        findSingle(count - 1, entities, findString);
    }

    private void findSingle(int idx, TestEntity[] entities, boolean findString) {
        TestEntity entity = entities[idx];
        Transaction transaction = store.beginTx();
        Cursor<TestEntity> cursor = transaction.createCursor(TestEntity.class);
        cursor.seek(1);
        long start = System.nanoTime();
        TestEntity foundEntity = findString ?
                cursor.find("simpleString", entity.getSimpleString()) :
                cursor.find("simpleLong", entity.getSimpleLong());
        long time = System.nanoTime() - start;
        cursor.close();
        transaction.abort();

        log("Found entity #" + idx + ": " + (time / 1000000) + " ms " +
                (time % 1000000) + " ns, " + (idx * 1000000000L / time) + " values/s");

        assertEqualEntity("Found", entity, foundEntity);
    }

    @Test
    public void testBulk_Indexed() {
        store = createBoxStore(true);
        bulkAll(100000, true);
    }

    @Test
    public void testBulk_NoIndex() {
        store = createBoxStore(false);
        bulkAll(100000, false);
    }

    private void bulkAll(int count, boolean useIndex) {
        String withOrWithoutIndex = useIndex ? "with index" : "without index";
        TestEntity[] entities = bulkInsert(count, withOrWithoutIndex, "My string ");
        bulkRead(count, entities);
        bulkUpdate(count, entities, withOrWithoutIndex, null);
        bulkUpdate(count, entities, withOrWithoutIndex, "Another fancy string ");
        bulkDelete(count, withOrWithoutIndex);
    }

    @Test
    public void testFindStringWithIndex() {
        int count = 100000;
        store = createBoxStore(true);
        TestEntity[] entities = bulkInsert(count, "with index", "ObjectBox Foo Bar x ");

        String[] stringsToLookup = new String[count];
        for (int i = 0; i < count; i++) {
            stringsToLookup[i] = entities[random.nextInt(count)].getSimpleString();
        }

        Transaction transaction = store.beginTx();
        long start = time();
        Cursor<TestEntity> cursor = transaction.createCursor(TestEntity.class);
        for (int i = 0; i < count; i++) {
            TestEntity testEntity = cursor.find("simpleString", stringsToLookup[i]);
            //assertEquals(stringsToLookup[i], testEntity.getSimpleString());
        }
        cursor.close();

        long time = time() - start;
        log("Looked up " + count + " entities (with index): " + time + " ms, " + valuesPerSec(count, time));
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////  Helper methods starting here /////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////

    private TestEntity createRandomTestEntity(String simpleString) {
        TestEntity e = new TestEntity();
        setScalarsToRandomValues(e);
        e.setSimpleString(simpleString);
        return e;
    }

    private void setScalarsToRandomValues(TestEntity entity) {
        entity.setSimpleInt(random.nextInt());
        entity.setSimpleLong(random.nextLong());
        entity.setSimpleBoolean(random.nextBoolean());
        entity.setSimpleDouble(random.nextDouble());
        entity.setSimpleFloat(random.nextFloat());
    }

    private TestEntity[] bulkInsert(int count, String withOrWithoutIndex, String stringValueBase) {
        TestEntity[] entities = new TestEntity[count];

        for (int i = 0; i < count; i++) {
            entities[i] = createRandomTestEntity(stringValueBase + i);
        }

        long time = putEntities(count, entities);
        log("Inserted " + count + " entities " + withOrWithoutIndex + ": " + time + " ms, " +
                valuesPerSec(count, time));

        return entities;
    }

    private long putEntities(int count, TestEntity[] entities) {
        long start = time();
        Transaction transaction = store.beginTx();
        Cursor<TestEntity> cursor = transaction.createCursor(TestEntity.class);

        for (int key = 1; key <= count; key++) {
            cursor.put(entities[key - 1]);
        }

        cursor.close();
        transaction.commit();
        return time() - start;
    }

    private void bulkRead(int count, TestEntity[] entities) {
        long time;
        TestEntity[] entitiesRead = new TestEntity[count];
        long start = time();
        Transaction transaction = store.beginReadTx();
        Cursor<TestEntity> cursor = transaction.createCursor(TestEntity.class);
        for (int key = 1; key <= count; key++) {
            entitiesRead[key - 1] = key == 1 ? cursor.get(1) : cursor.next();
        }
        cursor.close();
        transaction.abort();
        time = time() - start;
        log("Read " + count + " entities: " + time + "ms, " + valuesPerSec(count, time));

        for (int i = 0; i < count; i++) {
            String message = "Iteration " + i;
            TestEntity entity = entities[i];
            TestEntity testEntity = entitiesRead[i];
            assertEqualEntity(message, entity, testEntity);
        }
    }

    private void bulkUpdate(int count, TestEntity[] entities, String withOrWithoutIndex, String newStringBaseValue) {
        long time;// change all entities but not the indexed value
        for (int i = 0; i < count; i++) {
            setScalarsToRandomValues(entities[i]);
            if (newStringBaseValue != null) {
                entities[i].setSimpleString(newStringBaseValue + i);
            }
        }

        long start = time();
        Transaction transaction = store.beginTx();
        Cursor<TestEntity> cursor = transaction.createCursor(TestEntity.class);
        for (int key = 1; key <= count; key++) {
            cursor.put(entities[key - 1]);
        }
        cursor.close();
        transaction.commit();

        time = time() - start;
        String what = newStringBaseValue != null ? "scalars&strings" : "scalars";
        log("Updated " + what + " on " + count + " entities " + withOrWithoutIndex + ": " + time + " ms, "
                + valuesPerSec(count, time));
    }

    private void bulkDelete(int count, String withOrWithoutIndex) {
        long time;
        long start = time();
        Transaction transaction = store.beginTx();
        Cursor<TestEntity> cursor = transaction.createCursor(TestEntity.class);
        for (int key = 1; key <= count; key++) {
            cursor.deleteEntity(key);
        }
        cursor.close();
        transaction.commit();

        time = time() - start;
        log("Deleted " + count + " entities " + withOrWithoutIndex + ": " + time + " ms, " + valuesPerSec(count, time));
    }

    private void assertEqualEntity(String message, TestEntity expected, TestEntity actual) {
        assertNotNull(actual);
        assertEquals(message, expected.getId(), actual.getId());
        assertEquals(message, expected.getSimpleInt(), actual.getSimpleInt());
        assertEquals(message, expected.getSimpleBoolean(), actual.getSimpleBoolean());
        assertEquals(message, expected.getSimpleLong(), actual.getSimpleLong());
        assertEquals(message, expected.getSimpleFloat(), actual.getSimpleFloat(), 0.00000001);
        assertEquals(message, expected.getSimpleDouble(), actual.getSimpleDouble(), 0.00000001);
    }

    private String valuesPerSec(int count, long timeMillis) {
        return (timeMillis > 0 ? (count * 1000 / timeMillis) : "N/A") + " values/s";
    }

}
