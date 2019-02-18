/*
 * Copyright 2019 ObjectBox Ltd. All rights reserved.
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

import io.objectbox.reactive.DataObserver;
import io.objectbox.reactive.SubscriptionBuilder;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SynchronousObjectClassObserverTest extends AbstractObjectBoxTest {

    final List<Class> classesWithChanges = new ArrayList<>();

    DataObserver objectClassObserver = new DataObserver<Class>() {
        @Override
        public void onData(Class objectClass) {
            classesWithChanges.add(objectClass);
        }
    };

    @Override
    protected BoxStore createBoxStore() {
        return createBoxStoreBuilderWithTwoEntities(false, new SynchronousExecutorService())
                .build();
    }

    @Test
    public void transactionsAndObservationPerformedSequentially() {
        final SubscriptionBuilder<Class> subscriptionBuilder = this.store.subscribe();
        subscriptionBuilder.observer(objectClassObserver);

        assertEquals(2, classesWithChanges.size());
        assertTrue(classesWithChanges.contains(TestEntity.class));
        assertTrue(classesWithChanges.contains(TestEntityMinimal.class));

        classesWithChanges.clear();
        this.store.runInTx(new Runnable() {
            @Override
            public void run() {
                putTestEntities(3);
            }
        });

        assertEquals(1, classesWithChanges.size());
    }
}
