/*
 * Copyright © 2026 ObjectBox Ltd. <https://objectbox.io>
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

import org.junit.Test;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests {@link ObjectBoxThreadPoolExecutor}.
 */
public class ObjectBoxThreadPoolExecutorTest extends AbstractObjectBoxTest {

    @Test
    public void executor_cleansThreadResources() throws Exception {
        // Use a single thread to make it easy to check thread-local Box resources have been cleaned up
        ObjectBoxThreadPoolExecutor executor = store.newFixedThreadPoolExecutor(1);

        try {
            // Submit a runnable that uses a Box API that will create a thread-local reader cursor
            Future<Object[]> readResult = executor.submit(() -> {
                Box<TestEntity> box = store.boxFor(TestEntity.class);
                TestEntity entity = new TestEntity();
                entity.setSimpleString("executor-test");
                long id = box.put(entity);

                TestEntity testEntity = box.get(id); // Creates a thread-local reader cursor

                boolean hasReaderCursor = box.hasReaderCursorForCurrentThread();

                return new Object[]{testEntity, hasReaderCursor};
            });
            Object[] result = readResult.get(1, TimeUnit.SECONDS);

            TestEntity testEntity = (TestEntity) result[0];
            assertNotNull(testEntity);
            assertEquals("executor-test", testEntity.getSimpleString());

            // Verify that a thread-local reader cursor was created
            Boolean hasReaderCursor = (Boolean) result[1];
            assertTrue(hasReaderCursor);

            // Check the thread-local reader cursor has been released after the previous runnable was executed
            Future<Boolean> cleanedResult = executor.submit(() -> {
                Box<TestEntity> box = store.boxFor(TestEntity.class);
                return box.hasReaderCursorForCurrentThread();
            });
            assertFalse(cleanedResult.get(1, TimeUnit.SECONDS));
        } finally {
            executor.shutdown();
            assertTrue(executor.awaitTermination(1, TimeUnit.SECONDS));
        }
    }

    @Test
    public void newCachedThreadPoolExecutor_works() throws Exception {
        assertExecutor(store.newCachedThreadPoolExecutor());
        assertExecutor(store.newCachedThreadPoolExecutor(Executors.defaultThreadFactory()));
    }

    @Test
    public void newFixedThreadPoolExecutor_works() throws Exception {
        assertExecutor(store.newFixedThreadPoolExecutor(2));
        assertExecutor(store.newFixedThreadPoolExecutor(2, Executors.defaultThreadFactory()));
    }

    /**
     * Quickly checks the pre-configured executors works.
     */
    private void assertExecutor(ObjectBoxThreadPoolExecutor executor) throws Exception {
        try {
            // Create at least a write and a read transaction
            Future<TestEntity> future = executor.submit(() -> {
                Box<TestEntity> box = store.boxFor(TestEntity.class);
                TestEntity entity = new TestEntity();
                entity.setSimpleString("executor-test");
                long id = box.put(entity);

                return box.get(id);
            });
            TestEntity testEntity = future.get(1, TimeUnit.SECONDS);
            assertNotNull(testEntity);
            assertEquals("executor-test", testEntity.getSimpleString());
        } finally {
            executor.shutdown();
            assertTrue(executor.awaitTermination(1, TimeUnit.SECONDS));
        }
    }

}
