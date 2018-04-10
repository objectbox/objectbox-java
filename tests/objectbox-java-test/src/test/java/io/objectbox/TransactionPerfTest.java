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

import org.junit.Test;

public class TransactionPerfTest extends AbstractObjectBoxTest {

    public static final int COUNT = 100000;

    @Test
    public void testBoxManagedReaderPerformance() {
        Box<TestEntity> box = getTestEntityBox();
        TestEntity entity = new TestEntity();
        entity.setSimpleString("foobar");
        long id = box.put(entity);
        long start = System.currentTimeMillis();
        for (int i = 0; i < COUNT; i++) {
            box.get(id);
        }
        long time = System.currentTimeMillis() - start;
        log("Read with box: " + valuesPerSec(COUNT, time));
    }

    @Test
    public void testOneReadTxPerGetPerformance() {
        final Box<TestEntity> box = getTestEntityBox();
        TestEntity entity = new TestEntity();
        entity.setSimpleString("foobar");
        final long id = box.put(entity);
        long start = System.currentTimeMillis();
        for (int i = 0; i < COUNT; i++) {
            store.runInReadTx(new Runnable() {
                @Override
                public void run() {
                    box.get(id);
                }
            });
        }
        long time = System.currentTimeMillis() - start;
        log("Read with one TX per get: " + valuesPerSec(COUNT, time));
    }

    @Test
    public void testInsideSingleReadTxPerformance() {
        TestEntity entity = new TestEntity();
        entity.setSimpleString("foobar");
        final Box<TestEntity> box = getTestEntityBox();
        final long id = box.put(entity);
        store.runInReadTx(new Runnable() {
            @Override
            public void run() {
                long start = System.currentTimeMillis();
                for (int i = 0; i < COUNT; i++) {
                    box.get(id);
                }
                long time = System.currentTimeMillis() - start;
                log("Read with box inside read TX: " + valuesPerSec(COUNT, time));
            }
        });
    }

    private String valuesPerSec(int count, long timeMillis) {
        return count + " in " + timeMillis + ": " + (timeMillis > 0 ? (count * 1000 / timeMillis) : "N/A") + " values/s";
    }

}
