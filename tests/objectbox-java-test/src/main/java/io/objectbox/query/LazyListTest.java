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

package io.objectbox.query;

import org.junit.Test;

import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import io.objectbox.AbstractObjectBoxTest;
import io.objectbox.TestEntity;
import io.objectbox.TestEntity_;
import io.objectbox.exception.DbException;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class LazyListTest extends AbstractObjectBoxTest {

    @Test
    public void testSizeAndGetAndPeak() {
        List<TestEntity> list = putTestEntities(2);

        LazyList<TestEntity> listLazy = getTestEntityBox().query().build().findLazyCached();
        assertEquals(list.size(), listLazy.size());
        assertNull(listLazy.peek(0));
        assertNull(listLazy.peek(1));

        assertNotNull(listLazy.get(1));
        assertNull(listLazy.peek(0));
        assertNotNull(listLazy.peek(1));

        assertNotNull(listLazy.get(0));
        assertNotNull(listLazy.peek(0));
        assertNotNull(listLazy.peek(1));
    }

    @Test
    public void testGetAll100() {
        List<TestEntity> list = putTestEntities(100);
        LazyList<TestEntity> listLazy = getTestEntityBox().query().build().findLazyCached();
        assertIds(list, listLazy);
    }

    @Test

    public void testGetAll100Uncached() {
        List<TestEntity> list = putTestEntities(100);
        LazyList<TestEntity> listLazy = getTestEntityBox().query().build()
                .findLazy();
        assertIds(list, listLazy);
    }

    @Test
    public void testSublist() {
        List<TestEntity> list = putTestEntities(10);
        LazyList<TestEntity> listLazy = getTestEntityBox().query().build().findLazyCached();
        assertIds(list.subList(2, 7), listLazy.subList(2, 7));
    }

    @Test
    public void testSublistUncached() {
        List<TestEntity> list = putTestEntities(10);
        LazyList<TestEntity> listLazy = getTestEntityBox().query().build().findLazy();
        try {
            assertIds(list.subList(2, 7), listLazy.subList(2, 7));
        } catch (DbException e) {
            assertEquals("This operation only works with cached lazy lists", e.getMessage());
        }

    }

    @Test
    public void testIterator() {
        List<TestEntity> list = putTestEntities(100);
        LazyList<TestEntity> listLazy = getTestEntityBox().query().build().findLazyCached();
        testIterator(list, listLazy, false);
    }

    @Test
    public void testIteratorUncached() {
        List<TestEntity> list = putTestEntities(100);
        LazyList<TestEntity> listLazy = getTestEntityBox().query().build().findLazy();
        testIterator(list, listLazy, true);
    }

    protected void testIterator(List<TestEntity> list, LazyList<TestEntity> listLazy, boolean uncached) {
        ListIterator<TestEntity> iterator = listLazy.listIterator();
        try {
            iterator.previous();
            fail("previous should throw here");
        } catch (NoSuchElementException expected) {
            // OK
        }
        int size = list.size();
        for (int i = 0; i < size; i++) {
            assertTrue(iterator.hasNext());
            assertEquals(i > 0, iterator.hasPrevious());
            assertEquals(i, iterator.nextIndex());
            assertEquals(i - 1, iterator.previousIndex());

            if (i > 0) {
                TestEntity entityPrevious = list.get(i - 1);
                assertEquals(entityPrevious.getId(), iterator.previous().getId());
                iterator.next();
            }

            TestEntity entity = list.get(i);
            assertNull(listLazy.peek(i));
            TestEntity lazyEntity = iterator.next();
            if (uncached) {
                assertNull(listLazy.peek(i));
            } else {
                assertNotNull(listLazy.peek(i));
            }
            assertEquals(entity.getId(), lazyEntity.getId());
        }
        assertFalse(iterator.hasNext());
        try {
            iterator.next();
            fail("next should throw here");
        } catch (NoSuchElementException expected) {
            // OK
        }
    }

    @Test
    public void testEmpty() {
        putTestEntities(1);

        LazyList<TestEntity> listLazy = getTestEntityBox().query().equal(TestEntity_.simpleInt, -1).build().findLazyCached();
        assertTrue(listLazy.isEmpty());
        try {
            listLazy.get(0);
            fail("Not empty");
        } catch (RuntimeException e) {
            // Expected, OK
        }

    }

    @Test
    public void testUncached() {
        putTestEntities(1);

        LazyList<TestEntity> listLazy = getTestEntityBox().query().build().findLazy();
        assertFalse(listLazy.isEmpty());
        TestEntity entity1 = listLazy.get(0);
        TestEntity entity2 = listLazy.get(0);
        assertEquals(entity1.getId(), entity2.getId());
        try {
            listLazy.loadRemaining();
            fail("Not empty");
        } catch (DbException expected) {
            // Expected, OK
        }
    }


    protected void assertIds(List<TestEntity> list, List<TestEntity> list2) {
        for (int i = 0; i < list.size(); i++) {
            TestEntity entity = list.get(i);
            TestEntity lazyEntity = list2.get(i);
            assertIds(entity, lazyEntity);
        }
    }

    protected void assertIds(TestEntity entity, TestEntity entity2) {
        assertEquals(entity.getId(), entity2.getId());
    }
}
