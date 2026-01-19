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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * A {@link ThreadPoolExecutor} that automatically releases thread-local ObjectBox resources after each task execution
 * by calling {@link BoxStore#closeThreadResources()}.
 * <p>
 * This is useful when using a thread pool with ObjectBox to ensure that thread-local resources (currently readers only)
 * are properly cleaned up after each task completes.
 * <p>
 * <b>Recommended:</b> Use the factory methods {@link BoxStore#newFixedThreadPoolExecutor(int)} or
 * {@link BoxStore#newCachedThreadPoolExecutor()} to create instances of this executor.
 * <p>
 * Example usage:
 * <pre>
 * BoxStore boxStore = MyObjectBox.builder().build();
 *
 * // Recommended: Use BoxStore factory methods
 * ObjectBoxThreadPoolExecutor executor = boxStore.newFixedThreadPoolExecutor(4);
 *
 * // Or for a cached thread pool
 * ObjectBoxThreadPoolExecutor cachedExecutor = boxStore.newCachedThreadPoolExecutor();
 *
 * // Advanced: Direct construction for custom configuration
 * ObjectBoxThreadPoolExecutor customExecutor = new ObjectBoxThreadPoolExecutor(
 *     boxStore,
 *     4, // core pool size
 *     8, // maximum pool size
 *     60L, TimeUnit.SECONDS, // keep-alive time
 *     new LinkedBlockingQueue&lt;&gt;()
 * );
 * </pre>
 */
public class ObjectBoxThreadPoolExecutor extends ThreadPoolExecutor {

    private final BoxStore boxStore;

    /**
     * Creates a new ObjectBoxThreadPoolExecutor with the given parameters.
     * <p>
     * See {@link ThreadPoolExecutor#ThreadPoolExecutor(int, int, long, TimeUnit, BlockingQueue)} for parameter
     * details.
     *
     * @param boxStore the BoxStore instance for which to close thread resources
     */
    public ObjectBoxThreadPoolExecutor(BoxStore boxStore, int corePoolSize, int maximumPoolSize,
                                       long keepAliveTime, TimeUnit unit,
                                       BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
        this.boxStore = boxStore;
    }

    /**
     * Creates a new ObjectBoxThreadPoolExecutor with the given parameters.
     * <p>
     * See {@link ThreadPoolExecutor#ThreadPoolExecutor(int, int, long, TimeUnit, BlockingQueue, ThreadFactory)} for
     * parameter details.
     *
     * @param boxStore the BoxStore instance for which to close thread resources
     */
    public ObjectBoxThreadPoolExecutor(BoxStore boxStore, int corePoolSize, int maximumPoolSize,
                                       long keepAliveTime, TimeUnit unit,
                                       BlockingQueue<Runnable> workQueue,
                                       ThreadFactory threadFactory) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
        this.boxStore = boxStore;
    }

    /**
     * Creates a new ObjectBoxThreadPoolExecutor with the given parameters.
     * <p>
     * See
     * {@link ThreadPoolExecutor#ThreadPoolExecutor(int, int, long, TimeUnit, BlockingQueue, RejectedExecutionHandler)}
     * for parameter details.
     *
     * @param boxStore the BoxStore instance for which to close thread resources
     */
    public ObjectBoxThreadPoolExecutor(BoxStore boxStore, int corePoolSize, int maximumPoolSize,
                                       long keepAliveTime, TimeUnit unit,
                                       BlockingQueue<Runnable> workQueue,
                                       RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler);
        this.boxStore = boxStore;
    }

    /**
     * Creates a new ObjectBoxThreadPoolExecutor with the given parameters.
     * <p>
     * See
     * {@link ThreadPoolExecutor#ThreadPoolExecutor(int, int, long, TimeUnit, BlockingQueue, ThreadFactory,
     * RejectedExecutionHandler)} for parameter details.
     *
     * @param boxStore the BoxStore instance for which to close thread resources
     */
    public ObjectBoxThreadPoolExecutor(BoxStore boxStore, int corePoolSize, int maximumPoolSize,
                                       long keepAliveTime, TimeUnit unit,
                                       BlockingQueue<Runnable> workQueue,
                                       ThreadFactory threadFactory,
                                       RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
        this.boxStore = boxStore;
    }

    /**
     * Releases thread-local ObjectBox resources after each task execution.
     */
    @Override
    protected void afterExecute(Runnable runnable, Throwable throwable) {
        super.afterExecute(runnable, throwable);
        boxStore.closeThreadResources();
    }
}
