/*
 * Copyright (C) 2011-2016 Markus Junginger
 *
 * This file is part of greenDAO Generator.
 *
 * greenDAO Generator is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * greenDAO Generator is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with greenDAO Generator.  If not, see <http://www.gnu.org/licenses/>.
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
