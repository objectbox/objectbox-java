/*
 * Copyright 2017 ObjectBox Ltd. All rights reserved.
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
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class BoxTest extends AbstractObjectBoxTest {

    private Box<TestEntity> box;

    @Before
    public void setUpBox() {
        box = getTestEntityBox();
    }

    @Test
    public void testPutAndGet() {
        TestEntity entity = new TestEntity();
        entity.setSimpleInt(1977);
        long key = box.put(entity);
        assertTrue(key != 0);
        assertEquals(key, entity.getId());

        TestEntity entityRead = box.get(key);
        assertNotNull(entityRead);
        assertEquals(1977, entityRead.getSimpleInt());
    }

    @Test
    public void testPutGetUpdateGetRemove() {
        // create an entity
        TestEntity entity = new TestEntity();
        entity.setSimpleInt(1977);
        entity.setSimpleLong(54321);
        String value1 = "lulu321";
        entity.setSimpleString(value1);
        long key = box.put(entity);

        // get it
        TestEntity entityRead = box.get(key);
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
        entityRead = box.get(key);
        assertNotNull(entityRead);
        assertEquals(1977, entityRead.getSimpleInt());
        assertEquals(12345, entityRead.getSimpleLong());
        assertEquals(value2, entityRead.getSimpleString());

        // and remove it
        box.remove(key);
        assertNull(box.get(key));
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
    public void testRemoveMany() {
        List<TestEntity> entities = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            TestEntity entity = new TestEntity();
            entity.setSimpleInt(2000 + i);
            entities.add(entity);
        }
        box.put(entities);
        assertEquals(entities.size(), box.count());

        box.remove(entities.get(1));
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
        store.runInTx(new Runnable() {
            @Override
            public void run() {
                box.put(new TestEntity());
                counts[0] = box.count();
                box.put(new TestEntity());
                counts[1] = box.count();
            }
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
        box.put((Collection) null);
        box.put((TestEntity[]) null);
        box.remove((Collection) null);
        box.remove((long[]) null);
        box.removeByKeys(null);
    }

//    @Test
//    public void testFindString() {
//        putTestEntity("banana", 0);
//        putTestEntity("apple", 0);
//        putTestEntity("banana", 0);
//
//        List<TestEntity> list = box.find(TestEntity_.simpleString, "banana");
//        assertEquals(2, list.size());
//        assertEquals(1, list.get(0).getId());
//        assertEquals(3, list.get(1).getId());
//    }
//
//    @Test
//    public void testFindInt() {
//        putTestEntity(null, 42);
//        putTestEntity(null, 23);
//        putTestEntity(null, 42);
//
//        List<TestEntity> list = box.find(TestEntity_.simpleInt, 42);
//        assertEquals(2, list.size());
//        assertEquals(1, list.get(0).getId());
//        assertEquals(3, list.get(1).getId());
//    }

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
