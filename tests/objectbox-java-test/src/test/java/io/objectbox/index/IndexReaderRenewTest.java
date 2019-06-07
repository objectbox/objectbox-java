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

package io.objectbox.index;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.objectbox.AbstractObjectBoxTest;
import io.objectbox.Box;
import io.objectbox.BoxStore;
import io.objectbox.index.model.EntityLongIndex;
import io.objectbox.index.model.EntityLongIndex_;
import io.objectbox.index.model.MyObjectBox;
import io.objectbox.query.Query;
import io.objectbox.reactive.DataObserver;
import io.objectbox.reactive.DataTransformer;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class IndexReaderRenewTest extends AbstractObjectBoxTest {
    @Test
    public void testOverwriteIndexedValue() throws InterruptedException {
        final Box<EntityLongIndex> box = store.boxFor(EntityLongIndex.class);
        final int initialValue = 1;

        final EntityLongIndex[] transformResults = {null, null, null};
        final CountDownLatch transformLatch1 = new CountDownLatch(1);
        final CountDownLatch transformLatch2 = new CountDownLatch(1);
        final AtomicInteger transformerCallCount = new AtomicInteger();

        final Query<EntityLongIndex> query = box.query().equal(EntityLongIndex_.indexedLong, 0).build();
        store.subscribe(EntityLongIndex.class).transform(new DataTransformer<Class<EntityLongIndex>, EntityLongIndex>() {
            @Override
            public EntityLongIndex transform(Class<EntityLongIndex> clazz) throws Exception {
                int callCount = transformerCallCount.incrementAndGet();
                if (callCount == 1) {
                    query.setParameter(EntityLongIndex_.indexedLong, 1);
                    EntityLongIndex unique = query.findUnique();
                    transformLatch1.countDown();
                    return unique;
                } else if (callCount == 2) {
                    query.setParameter(EntityLongIndex_.indexedLong, 1);
                    transformResults[0] = query.findUnique();
                    transformResults[1] = query.findUnique();
                    query.setParameter(EntityLongIndex_.indexedLong, 0);
                    transformResults[2] = query.findUnique();
                    transformLatch2.countDown();
                    return transformResults[0];
                } else {
                    throw new RuntimeException("Unexpected: " + callCount);
                }
            }
        }).observer(new DataObserver<EntityLongIndex>() {
            @Override
            public void onData(EntityLongIndex data) {
                // Dummy
            }
        });

        assertTrue(transformLatch1.await(5, TimeUnit.SECONDS));
        box.put(createEntityLongIndex(initialValue));

        assertTrue(transformLatch2.await(5, TimeUnit.SECONDS));
        assertEquals(2, transformerCallCount.intValue());

        assertNotNull(transformResults[0]);
        assertNotNull(transformResults[1]);
        assertNull(transformResults[2]);

        query.setParameter(EntityLongIndex_.indexedLong, initialValue);
        assertNotNull(query.findUnique());

        query.setParameter(EntityLongIndex_.indexedLong, initialValue);
        assertNotNull(query.findUnique());
        assertNotNull(query.findUnique());
    }

    private EntityLongIndex createEntityLongIndex(int initialValue) {
        EntityLongIndex entity = new EntityLongIndex();
        entity.setId(1);
        entity.setIndexedLong(initialValue);
        entity.setFloat1(7F);
        entity.setFloat2(10F);
        return entity;
    }

    @Test
    public void testOldReaderInThread() throws InterruptedException {
        final Box<EntityLongIndex> box = store.boxFor(EntityLongIndex.class);
        final int initialValue = 1;

        final EntityLongIndex[] results = new EntityLongIndex[5];
        final CountDownLatch latchRead1 = new CountDownLatch(1);
        final CountDownLatch latchPut = new CountDownLatch(1);
        final CountDownLatch latchRead2 = new CountDownLatch(1);
        final Query<EntityLongIndex> query = box.query().equal(EntityLongIndex_.indexedLong, 0).build();

        new Thread() {
            @Override
            public void run() {
                query.setParameter(EntityLongIndex_.indexedLong, initialValue);
                EntityLongIndex unique = query.findUnique();
                assertNull(unique);
                latchRead1.countDown();
                System.out.println("BEFORE put: " + box.getReaderDebugInfo());
                System.out.println("count before: " + box.count());

                try {
                    latchPut.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
                System.out.println("AFTER put: " + box.getReaderDebugInfo());
                System.out.println("count after: " + box.count());

                query.setParameter(EntityLongIndex_.indexedLong, initialValue);
                results[0] = query.findUnique();
                results[1] = box.get(1);
                results[2] = query.findUnique();
                query.setParameter(EntityLongIndex_.indexedLong, 0);
                results[3] = query.findUnique();
                latchRead2.countDown();
                box.closeThreadResources();
            }
        }.start();

        assertTrue(latchRead1.await(5, TimeUnit.SECONDS));
        box.put(createEntityLongIndex(initialValue));
        latchPut.countDown();

        assertTrue(latchRead2.await(5, TimeUnit.SECONDS));

        assertNotNull(results[1]);
        assertNotNull(results[0]);
        assertNotNull(results[2]);
        assertNull(results[3]);

        query.setParameter(EntityLongIndex_.indexedLong, initialValue);
        assertNotNull(query.findUnique());

        query.setParameter(EntityLongIndex_.indexedLong, initialValue);
        assertNotNull(query.findUnique());
        assertNotNull(query.findUnique());
    }

    @Test
    public void testOldReaderWithIndex() throws InterruptedException {
        final Box<EntityLongIndex> box = store.boxFor(EntityLongIndex.class);
        final int initialValue = 1;

        final Query<EntityLongIndex> query = box.query().equal(EntityLongIndex_.indexedLong, 0).build();
        assertNull(query.findUnique());
        System.out.println("BEFORE put: " + box.getReaderDebugInfo());
        System.out.println("count before: " + box.count());

        box.put(createEntityLongIndex(initialValue));

        System.out.println("AFTER put: " + box.getReaderDebugInfo());
        System.out.println("count after: " + box.count());

        query.setParameter(EntityLongIndex_.indexedLong, initialValue);
        assertNotNull(query.findUnique());

        query.setParameter(EntityLongIndex_.indexedLong, 0);
        assertNull(query.findUnique());
    }

    @Override
    protected BoxStore createBoxStore() {
        return MyObjectBox.builder().directory(boxStoreDir).build();
    }
}
