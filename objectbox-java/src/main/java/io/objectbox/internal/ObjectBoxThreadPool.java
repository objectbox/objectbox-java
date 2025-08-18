/*
 * Copyright 2017 ObjectBox Ltd.
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

package io.objectbox.internal;

import java.util.concurrent.Executors;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.TimeUnit;

import io.objectbox.BoxStore;
import io.objectbox.annotation.apihint.Internal;

/**
 * Custom executor service similar to {@link Executors#newWorkStealingPool()} with the following adjustments:
 * <ul>
 *     <li>Release thread local resources ({@link BoxStore#closeThreadResources()}) after task execution</li>
 *     <li>Uses a custom thread factory to name threads like "ObjectBox-ForkJoinPool-1-Thread-1"</li>
 * </ul>
 */
@Internal
public final class ObjectBoxThreadPool extends AbstractExecutorService {
    private final BoxStore boxStore;
    private final ExecutorService executorImpl;

    public ObjectBoxThreadPool(BoxStore boxStore, int parallelism) {
        this.boxStore = boxStore;
        this.executorImpl = Executors.unconfigurableExecutorService(
            new ForkJoinPool(
                parallelism, 
                pool -> {
                    ForkJoinWorkerThread thread = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
                    // Priority and daemon status are inherited from calling thread; ensure to reset if required
                    if (thread.getPriority() != Thread.NORM_PRIORITY) {
                        thread.setPriority(Thread.NORM_PRIORITY);
                    }
                    if (thread.isDaemon()) {
                        thread.setDaemon(false);
                    }
                    thread.setName("ObjectBox-" + thread.getName());
                    return thread;
                },
                null,
                false));
    }


    @Override
    public void shutdown() {
        executorImpl.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return executorImpl.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return executorImpl.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return executorImpl.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return executorImpl.awaitTermination(timeout, unit);
    }

    @Override
    public void execute(Runnable command) {
        executorImpl.execute(() -> {
            try {
                command.run();
            } finally {
                boxStore.closeThreadResources();
            }
        });
    }
}
