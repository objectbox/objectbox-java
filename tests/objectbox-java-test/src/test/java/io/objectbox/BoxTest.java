/*
 * Copyright 2017-2025 ObjectBox Ltd.
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

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;


import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class BoxTest extends AbstractObjectBoxTest {

    private Box<TestEntity> box;

    @Before
    public void setUpBox() {
        box = getTestEntityBox();
    }

    @Test
    public void testPutAndGet() {
        final String simpleString = "sunrise";
        final int simpleInt = 1977;

        TestEntity entity = createTestEntity(simpleString, simpleInt);
        long id = box.put(entity);
        assertTrue(id != 0);
        assertEquals(id, entity.getId());

        short valShort = 100 + simpleInt;
        long valLong = 1000 + simpleInt;
        float valFloat = 200 + simpleInt / 10f;
        double valDouble = 2000 + simpleInt / 100f;
        byte[] valByteArray = {1, 2, (byte) simpleInt};

        TestEntity entityRead = box.get(id);
        assertNotNull(entityRead);
        assertEquals(id, entityRead.getId());
        assertEquals(simpleString, entityRead.getSimpleString());
        assertEquals(simpleInt, entityRead.getSimpleInt());
        assertEquals((byte) (10 + simpleInt), entityRead.getSimpleByte());
        assertFalse(entityRead.getSimpleBoolean());
        assertEquals(valShort, entityRead.getSimpleShort());
        assertEquals(valLong, entityRead.getSimpleLong());
        assertEquals(valFloat, entityRead.getSimpleFloat(), 0);
        assertEquals(valDouble, entityRead.getSimpleDouble(), 0);
        assertArrayEquals(valByteArray, entityRead.getSimpleByteArray());
        String[] expectedStringArray = new String[]{simpleString};
        assertArrayEquals(expectedStringArray, entityRead.getSimpleStringArray());
        assertEquals(Arrays.asList(expectedStringArray), entityRead.getSimpleStringList());
        assertEquals((short) (100 + simpleInt), entityRead.getSimpleShortU());
        assertEquals(simpleInt, entityRead.getSimpleIntU());
        assertEquals(1000 + simpleInt, entityRead.getSimpleLongU());
        assertEquals(1, entityRead.getStringObjectMap().size());
        assertEquals(simpleString, entityRead.getStringObjectMap().get(simpleString));
        assertEquals(simpleString, entityRead.getFlexProperty());
        assertArrayEquals(new boolean[]{false, false, true}, entity.getBooleanArray());
        assertArrayEquals(new short[]{(short) -valShort, valShort}, entity.getShortArray());
        assertArrayEquals(simpleString.toCharArray(), entity.getCharArray());
        assertArrayEquals(new int[]{-simpleInt, simpleInt}, entity.getIntArray());
        assertArrayEquals(new long[]{-valLong, valLong}, entity.getLongArray());
        assertArrayEquals(new float[]{-valFloat, valFloat}, entityRead.getFloatArray(), 0);
        assertArrayEquals(new double[]{-valDouble, valDouble}, entity.getDoubleArray(), 0);
        assertEquals(new Date(1000 + simpleInt), entity.getDate());
    }

    @Test
    public void testPutAndGet_defaultOrNullValues() {
        long id = box.put(new TestEntity());

        TestEntity defaultEntity = box.get(id);
        assertNull(defaultEntity.getSimpleString());
        assertEquals(0, defaultEntity.getSimpleInt());
        assertEquals((byte) 0, defaultEntity.getSimpleByte());
        assertFalse(defaultEntity.getSimpleBoolean());
        assertEquals((short) 0, defaultEntity.getSimpleShort());
        assertEquals(0, defaultEntity.getSimpleLong());
        assertEquals(0, defaultEntity.getSimpleFloat(), 0);
        assertEquals(0, defaultEntity.getSimpleDouble(), 0);
        assertNull(defaultEntity.getSimpleByteArray());
        assertNull(defaultEntity.getSimpleStringArray());
        assertNull(defaultEntity.getSimpleStringList());
        assertEquals(0, defaultEntity.getSimpleShortU());
        assertEquals(0, defaultEntity.getSimpleIntU());
        assertEquals(0, defaultEntity.getSimpleLongU());
        assertNull(defaultEntity.getStringObjectMap());
        assertNull(defaultEntity.getFlexProperty());
        assertNull(defaultEntity.getBooleanArray());
        assertNull(defaultEntity.getShortArray());
        assertNull(defaultEntity.getCharArray());
        assertNull(defaultEntity.getIntArray());
        assertNull(defaultEntity.getLongArray());
        assertNull(defaultEntity.getFloatArray());
        assertNull(defaultEntity.getDoubleArray());
        assertNull(defaultEntity.getDate());
    }

    // Note: There is a similar test using the Cursor API directly (which is deprecated) in CursorTest.
    @Test
    public void testPut_notAssignedId_fails() {
        TestEntity entity = new TestEntity();
        // Set ID that was not assigned
        entity.setId(1);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> box.put(entity));
        assertEquals("ID is higher or equal to internal ID sequence: 1 (vs. 1). Use ID 0 (zero) to insert new objects.", ex.getMessage());
    }

    @Test
    public void testPut_assignedId_inserts() {
        long id = box.put(new TestEntity());
        box.remove(id);
        // Put with previously assigned ID should insert
        TestEntity entity = new TestEntity();
        entity.setId(id);
        box.put(entity);
        assertEquals(1L, box.count());
    }

    @Test
    public void testPutStrings_withNull_ignoresNull() {
        final String[] stringArray = new String[]{"sunrise", null, "sunset"};
        final String[] expectedStringArray = new String[]{"sunrise", "sunset"};

        TestEntity entity = new TestEntity();
        entity.setSimpleStringArray(stringArray);
        entity.setSimpleStringList(Arrays.asList(stringArray));
        box.put(entity);

        TestEntity entityRead = box.get(entity.getId());
        assertNotNull(entityRead);
        assertArrayEquals(expectedStringArray, entityRead.getSimpleStringArray());
        assertEquals(Arrays.asList(expectedStringArray), entityRead.getSimpleStringList());
    }

    @Test
    public void testPutStrings_onlyNull_isEmpty() {
        TestEntity entity = new TestEntity();
        final String[] stringArray = new String[]{null};
        entity.setSimpleStringArray(stringArray);
        entity.setSimpleStringList(Arrays.asList(stringArray));
        box.put(entity);

        TestEntity entityRead = box.get(entity.getId());
        assertNotNull(entityRead);
        assertArrayEquals(new String[]{}, entityRead.getSimpleStringArray());
        assertEquals(new ArrayList<>(), entityRead.getSimpleStringList());
    }

    @Test
    public void testPutGetUpdateGetRemove() {
        // create an entity
        TestEntity entity = new TestEntity();
        entity.setSimpleInt(1977);
        entity.setSimpleLong(54321);
        String value1 = "lulu321";
        entity.setSimpleString(value1);
        long id = box.put(entity);

        // get it
        TestEntity entityRead = box.get(id);
        assertNotNull(entityRead);
        assertEquals(1977, entityRead.getSimpleInt());
        assertEquals(54321, entityRead.getSimpleLong());
        assertEquals(value1, entityRead.getSimpleString());

        // put with changed values
        String value2 = "lala123";
        entityRead.setSimpleString(value2);
        entityRead.setSimpleLong(12345);
        box.put(entityRead);

        // get the changed entity
        entityRead = box.get(id);
        assertNotNull(entityRead);
        assertEquals(1977, entityRead.getSimpleInt());
        assertEquals(12345, entityRead.getSimpleLong());
        assertEquals(value2, entityRead.getSimpleString());

        // and remove it
        assertTrue(box.contains(id));
        assertTrue(box.remove(id));
        assertNull(box.get(id));
        assertFalse(box.remove(id));
        assertFalse(box.contains(id));
    }

    @Test
    public void testPutManyAndGetAll() {
        List<TestEntity> entities = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            TestEntity entity = new TestEntity();
            entity.setSimpleInt(2000 + i);
            entities.add(entity);
        }
        box.put(entities);
        assertEquals(entities.size(), box.count());

        List<TestEntity> entitiesRead = box.getAll();
        assertEquals(entities.size(), entitiesRead.size());

        for (int i = 0; i < entities.size(); i++) {
            assertEquals(2000 + i, entitiesRead.get(i).getSimpleInt());
        }
    }

    @Test
    public void testPutBatched() {
        List<TestEntity> entities = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            TestEntity entity = new TestEntity();
            entity.setSimpleInt(2000 + i);
            entities.add(entity);
        }
        box.putBatched(entities, 4);
        assertEquals(entities.size(), box.count());

        List<TestEntity> entitiesRead = box.getAll();
        assertEquals(entities.size(), entitiesRead.size());

        for (int i = 0; i < entities.size(); i++) {
            assertEquals(2000 + i, entitiesRead.get(i).getSimpleInt());
        }
    }

    @Test
    public void testRemoveMany() {
        List<TestEntity> entities = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            TestEntity entity = new TestEntity();
            entity.setSimpleInt(2000 + i);
            entities.add(entity);
        }
        box.put(entities);
        assertEquals(entities.size(), box.count());

        assertTrue(box.remove(entities.get(1)));
        assertFalse(box.remove(entities.get(1)));
        assertEquals(entities.size() - 1, box.count());
        box.remove(entities.get(4), entities.get(5));
        assertEquals(entities.size() - 3, box.count());
        List<TestEntity> entitiesRemove = new ArrayList<>();
        entitiesRemove.add(entities.get(2));
        entitiesRemove.add(entities.get(8));
        entitiesRemove.add(entities.get(7));
        box.remove(entitiesRemove);
        assertEquals(entities.size() - 6, box.count());

        List<TestEntity> entitiesRead = box.getAll();
        assertEquals(entities.size() - 6, entitiesRead.size());

        assertEquals(2000, entitiesRead.get(0).getSimpleInt());
        assertEquals(2003, entitiesRead.get(1).getSimpleInt());
        assertEquals(2006, entitiesRead.get(2).getSimpleInt());
        assertEquals(2009, entitiesRead.get(3).getSimpleInt());

        box.removeAll();
        assertEquals(0, box.count());
    }

    // https://github.com/objectbox/objectbox-java/issues/626
    @Test
    public void testGetAllAfterGetAndRemove() {
        assertEquals(0, box.count());
        assertEquals(0, box.getAll().size());

        System.out.println("PUT");
        List<TestEntity> entities = putTestEntities(10);

        // explicitly get an entity (any will do)
        System.out.println("GET");
        TestEntity entity = box.get(entities.get(1).getId());
        assertNotNull(entity);

        System.out.println("REMOVE_ALL");
        box.removeAll();

        System.out.println("COUNT");
        assertEquals(0, box.count());
        System.out.println("GET_ALL");
        List<TestEntity> all = box.getAll();
        // note only 1 entity is returned by getAll, it is the one we explicitly get (last) above
        assertEquals(0, all.size());
    }

    @Test
    public void testPanicModeRemoveAllObjects() {
        assertEquals(0, box.panicModeRemoveAll());
        putTestEntities(7);
        assertEquals(7, box.panicModeRemoveAll());
        assertEquals(0, box.count());
    }

    @Test
    public void testRunInTx() {
        final long[] counts = {0, 0};
        store.runInTx(() -> {
            box.put(new TestEntity());
            counts[0] = box.count();
            box.put(new TestEntity());
            counts[1] = box.count();
        });
        assertEquals(1, counts[0]);
        assertEquals(2, counts[1]);
        assertEquals(2, box.count());
    }

    @Test
    public void testPutAndGetTwoEntities() {
        store.close();
        store.deleteAllFiles();
        store = createBoxStoreBuilderWithTwoEntities(false).build();
        box = store.boxFor(TestEntity.class);

        long key = putTestEntity(null, 1977).getId();
        TestEntity entityRead = box.get(key);
        assertEquals(1977, entityRead.getSimpleInt());

        Box<TestEntityMinimal> box2 = store.boxFor(TestEntityMinimal.class);
        TestEntityMinimal entity2 = new TestEntityMinimal();
        entity2.setText("foo");
        long key2 = box2.put(entity2);
        TestEntityMinimal entity2Read = box2.get(key2);
        assertEquals("foo", entity2Read.getText());
    }

    @Test
    public void testGetIds() {
        List<TestEntity> entities = putTestEntities(5);

        List<Long> ids = new ArrayList<>();
        ids.add(entities.get(1).getId());
        ids.add(entities.get(3).getId());
        List<TestEntity> readEntities = box.get(ids);
        assertEquals(2, readEntities.size());
        assertEquals((long) ids.get(0), readEntities.get(0).getId());
        assertEquals((long) ids.get(1), readEntities.get(1).getId());

        Map<Long, TestEntity> map = box.getMap(ids);
        assertEquals(2, map.size());
        assertEquals((long) ids.get(0), map.get(ids.get(0)).getId());
        assertEquals((long) ids.get(1), map.get(ids.get(1)).getId());
    }

    @Test
    public void testTwoReaders() {
        store.close();
        store.deleteAllFiles();
        store = createBoxStoreBuilderWithTwoEntities(false).build();
        box = store.boxFor(TestEntity.class);
        box.count();

        Box<TestEntityMinimal> box2 = store.boxFor(TestEntityMinimal.class);
        box2.count();
        box.count();
    }

    @Test
    public void testCollectionsNull() {
        box.put((Collection<TestEntity>) null);
        box.put((TestEntity[]) null);
        box.remove((Collection<TestEntity>) null);
        box.remove((long[]) null);
        box.removeByIds(null);
    }

    @Test
    public void testGetId() {
        TestEntity entity = putTestEntity(null, 42);
        assertTrue(entity.getId() > 0);
        assertEquals(entity.getId(), box.getId(entity));
    }

    @Test
    public void testCountMaxAndIsEmpty() {
        assertTrue(box.isEmpty());
        putTestEntity("banana", 0);
        assertFalse(box.isEmpty());

        assertEquals(1, box.count(1));
        assertEquals(1, box.count(2));
        putTestEntity("apple", 0);
        assertEquals(2, box.count(2));
        assertEquals(2, box.count(3));

        box.removeAll();
        assertTrue(box.isEmpty());
    }

}
