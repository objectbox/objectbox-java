package io.objectbox.internal;

import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.objectbox.BoxStore;
import io.objectbox.annotation.apihint.Internal;

/**
 * Custom thread pool similar to {@link Executors#newCachedThreadPool()} with the following adjustments:
 * <ul>
 *     <li>Release thread local resources ({@link BoxStore#closeThreadResources()})</li>
 *     <li>Reduce keep-alive time for threads to 20 seconds</li>
 *     <li>Uses a ThreadFactory to name threads like "ObjectBox-1-Thread-1"</li>
 * </ul>
 *
 */
@Internal
public class ObjectBoxThreadPool extends ThreadPoolExecutor {
    private final BoxStore boxStore;

    public ObjectBoxThreadPool(BoxStore boxStore) {
        super(0, Integer.MAX_VALUE, 20L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(),
                new ObjectBoxThreadFactory());
        this.boxStore = boxStore;
    }

    @Override
    protected void afterExecute(Runnable runnable, Throwable throwable) {
        super.afterExecute(runnable, throwable);
        boxStore.closeThreadResources();
    }

    static class ObjectBoxThreadFactory implements ThreadFactory {
        private static final AtomicInteger POOL_COUNT = new AtomicInteger();

        private final ThreadGroup group;
        private final String namePrefix = "ObjectBox-" + POOL_COUNT.incrementAndGet() + "-Thread-";
        private final AtomicInteger threadCount = new AtomicInteger();

        ObjectBoxThreadFactory() {
            SecurityManager securityManager = System.getSecurityManager();
            group = (securityManager != null) ? securityManager.getThreadGroup() :
                    Thread.currentThread().getThreadGroup();
        }

        public Thread newThread(Runnable runnable) {
            String name = namePrefix + threadCount.incrementAndGet();
            Thread thread = new Thread(group, runnable, name);

            // Priority and daemon status are inherited from calling thread; ensure to reset if required
            if (thread.getPriority() != Thread.NORM_PRIORITY) {
                thread.setPriority(Thread.NORM_PRIORITY);
            }
            if (thread.isDaemon()) {
                thread.setDaemon(false);
            }
            return thread;
        }
    }
}
