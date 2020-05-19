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

import org.greenrobot.essentials.collections.MultimapSet;
import org.greenrobot.essentials.collections.MultimapSet.SetType;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Set;

import javax.annotation.Nullable;

import io.objectbox.annotation.apihint.Internal;
import io.objectbox.reactive.DataObserver;
import io.objectbox.reactive.DataPublisher;
import io.objectbox.reactive.DataPublisherUtils;

@Internal
class ObjectClassPublisher implements DataPublisher<Class>, Runnable {
    final BoxStore boxStore;
    final MultimapSet<Integer, DataObserver<Class>> observersByEntityTypeId = MultimapSet.create(SetType.THREAD_SAFE);
    final Deque<int[]> changesQueue = new ArrayDeque<>();
    volatile boolean changePublisherRunning;

    ObjectClassPublisher(BoxStore boxStore) {
        this.boxStore = boxStore;
    }

    @Override
    public void subscribe(DataObserver<Class> observer, @Nullable Object forClass) {
        if (forClass == null) {
            for (int entityTypeId : boxStore.getAllEntityTypeIds()) {
                observersByEntityTypeId.putElement(entityTypeId, observer);
            }
        } else {
            int entityTypeId = boxStore.getEntityTypeIdOrThrow((Class<?>) forClass);
            observersByEntityTypeId.putElement(entityTypeId, observer);
        }
    }

    /**
     * Removes the given observer from all object classes it added itself to earlier (forClass == null).
     * This also considers weakly added observers.
     */
    public void unsubscribe(DataObserver<Class> observer, @Nullable Object forClass) {
        if (forClass != null) {
            int entityTypeId = boxStore.getEntityTypeIdOrThrow((Class<?>) forClass);
            unsubscribe(observer, entityTypeId);
        } else {
            for (int entityTypeId : boxStore.getAllEntityTypeIds()) {
                unsubscribe(observer, entityTypeId);
            }
        }
    }

    private void unsubscribe(DataObserver<Class> observer, int entityTypeId) {
        Set<DataObserver<Class>> observers = observersByEntityTypeId.get(entityTypeId);
        DataPublisherUtils.removeObserverFromCopyOnWriteSet(observers, observer);
    }

    @Override
    public void publishSingle(final DataObserver<Class> observer, @Nullable final Object forClass) {
        boxStore.internalScheduleThread(() -> {
            Collection<Class<?>> entityClasses = forClass != null ? Collections.singletonList((Class<?>) forClass) :
                    boxStore.getAllEntityClasses();
            for (Class<?> entityClass : entityClasses) {
                try {
                    observer.onData(entityClass);
                } catch (RuntimeException e) {
                    handleObserverException(entityClass);
                }
            }
        });
    }

    private void handleObserverException(Class objectClass) {
        RuntimeException newEx = new RuntimeException(
                "Observer failed while processing data for " + objectClass +
                        ". Consider using an ErrorObserver");
        // So it won't be swallowed by thread pool
        newEx.printStackTrace();
        throw newEx;
    }

    /**
     * Non-blocking: will just enqueue the changes for a separate thread.
     */
    void publish(int[] entityTypeIdsAffected) {
        synchronized (changesQueue) {
            changesQueue.add(entityTypeIdsAffected);
            // Only one thread at a time
            if (!changePublisherRunning) {
                changePublisherRunning = true;
                boxStore.internalScheduleThread(this);
            }
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                // We do not join all available array, just in case the app relies on a specific order
                int[] entityTypeIdsAffected;
                synchronized (changesQueue) {
                    entityTypeIdsAffected = changesQueue.pollFirst();
                    if (entityTypeIdsAffected == null) {
                        changePublisherRunning = false;
                        break;
                    }
                }
                for (int entityTypeId : entityTypeIdsAffected) {
                    Collection<DataObserver<Class>> observers = observersByEntityTypeId.get(entityTypeId);
                    if (observers != null && !observers.isEmpty()) {
                        Class<?> objectClass = boxStore.getEntityClassOrThrow(entityTypeId);
                        try {
                            for (DataObserver<Class> observer : observers) {
                                observer.onData(objectClass);
                            }
                        } catch (RuntimeException e) {
                            handleObserverException(objectClass);
                        }
                    }
                }
            }
        } finally {
            // Just in Case of exceptions; it's better done within synchronized for regular cases
            changePublisherRunning = false;
        }
    }
}
