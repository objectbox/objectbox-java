package io.objectbox;

import java.io.Closeable;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

public class BoxStore implements Closeable {
    static {
        LibInit.init();
    }

    static native long nativeCreate(String directory, long maxDbSizeInKByte, byte[] model);

    static native void nativeDelete(long store);

    static native void nativeDropAllData(long store);

    static native long nativeBeginTx(long store);

    static native long nativeBeginReadTx(long store);

    static native long nativeCreateIndex(long store, String name, int entityId, int propertyId);

    private final File directory;
    private final long store;
    private final Map<Class, String> entityNameByClass;
    private final Map<Class, Class<Cursor>> entityCursorClassByClass;
    private final Map<Class, Box> boxes = new ConcurrentHashMap<>();
    private final Set<Transaction> transactions = Collections.newSetFromMap(new WeakHashMap<Transaction, Boolean>());

    /** Set when running inside TX */
    final ThreadLocal<Transaction> activeTx = new ThreadLocal<>();

    private boolean closed;

    Object txCommitCountLock = new Object();
    // Not atomic because it is read most of the time
    volatile int commitCount;

    BoxStore(BoxStoreBuilder builder) {
        this.directory = builder.directory;
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                throw new RuntimeException("Could not create directory: " +
                        directory.getAbsolutePath());
            }
        }
        if (!directory.isDirectory()) {
            throw new RuntimeException("Is not a directory: " + directory.getAbsolutePath());
        }
        store = nativeCreate(directory.getAbsolutePath(), builder.maxSizeInKByte, builder.model);
        entityNameByClass = new HashMap<>();
        entityCursorClassByClass = new HashMap<>();
    }

    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

    private void checkOpen() {
        if (closed) {
            throw new IllegalStateException("Store is closed");
        }
    }

    public <T> void registerEntityClass(String entityName, Class<T> entityClass, Class<? extends Cursor<T>> cursorClass) {
        entityNameByClass.put(entityClass, entityName);
        entityCursorClassByClass.put(entityClass, (Class) cursorClass);
    }

    String getEntityName(Class entityClass) {
        return entityNameByClass.get(entityClass);
    }

    <T> Class<Cursor<T>> getEntityCursorClass(Class<T> entityClass) {
        return (Class) entityCursorClassByClass.get(entityClass);
    }

    public Transaction beginTx() {
        checkOpen();
        // Because write TXs are typically not cached, initialCommitCount is not as relevant than for read TXs.
        int initialCommitCount = commitCount;
        long nativeTx = nativeBeginTx(store);
        Transaction tx = new Transaction(this, nativeTx, initialCommitCount);
        synchronized (transactions) {
            transactions.add(tx);
        }
        return tx;
    }

    public Transaction beginReadTx() {
        checkOpen();
        // initialCommitCount should be acquired before starting the tx. In race conditions, there is a chance the
        // commitCount is already outdated. That's OK because it only gives a false positive for an TX being obsolete.
        // In contrast, a false negative would make a TX falsely not considered obsolete, and thus readers would not be
        // updated resulting in querying obsolete data until another commit is done.
        // TODO add multithreaded test for this
        int initialCommitCount = commitCount;
        long nativeTx = nativeBeginReadTx(store);
        Transaction tx = new Transaction(this, nativeTx, initialCommitCount);
        synchronized (transactions) {
            transactions.add(tx);
        }
        return tx;
    }

    public boolean isClosed() {
        return closed;
    }

    public synchronized void close() {
        if (!closed) {
            closed = true;
            List<Transaction> transactionsToClose;
            synchronized (transactions) {
                transactionsToClose = new ArrayList<>(this.transactions);
            }
            for (Transaction t : transactionsToClose) {
                t.close();
            }
            nativeDelete(store);
        }
    }

    public boolean deleteAllFiles() {
        if (!closed) {
            throw new IllegalStateException("Store must be closed");
        }
        return deleteAllFiles(directory);
    }

    public static boolean deleteAllFiles(File objectStoreDirectory) {
        boolean ok = true;
        if (objectStoreDirectory != null && objectStoreDirectory.exists()) {
            File[] files = objectStoreDirectory.listFiles();
            if (files != null) {
                for (File file : files) {
                    ok &= file.delete();
                }
            } else {
                ok = false;
            }
            ok &= objectStoreDirectory.delete();
        }
        return ok;
    }

    public void unregisterTransaction(Transaction transaction) {
        synchronized (transactions) {
            transactions.remove(transaction);
        }
    }

    public void dropAllData() {
        nativeDropAllData(store);
    }

    public void txCommitted(Transaction tx) {
        // Only one write TX at a time, but there is a chance two writers race after commit: thus synchronize
        synchronized (txCommitCountLock) {
            commitCount++; // Overflow is OK because we check for equality
        }

        for (Box box : boxes.values()) {
            box.txCommitted(tx);
        }
    }

    public <T> Box<T> boxFor(Class<T> entityClass) {
        Box box = boxes.get(entityClass);
        if (box == null) {
            // Ensure a box is created just once
            synchronized (boxes) {
                box = boxes.get(entityClass);
                if (box == null) {
                    box = new Box<>(this, entityClass);
                    boxes.put(entityClass, box);
                }
            }
        }
        return box;
    }

    public void runInTx(Runnable runnable) {
        Transaction tx = this.activeTx.get();
        // Only if not already set, allowing to call it recursively with first (outer) TX
        if (tx == null) {
            tx = beginTx();
            activeTx.set(tx);
            try {
                runnable.run();
                tx.commit();
            } finally {
                activeTx.remove();
                tx.close();
            }
        } else {
            runnable.run();
        }
    }

    public <R> R callInTx(Callable<R> callable) throws Exception {
        Transaction tx = this.activeTx.get();
        // Only if not already set, allowing to call it recursively with first (outer) TX
        if (tx == null) {
            tx = beginTx();
            activeTx.set(tx);
            try {
                R result = callable.call();
                tx.commit();
                return result;
            } finally {
                activeTx.remove();
                tx.close();
            }
        } else {
            return callable.call();
        }
    }

    public void runInTxAsync(final Runnable runnable, final TxCallback<Void> callback) {
        new Thread() {
            @Override
            public void run() {
                try {
                    runInTx(runnable);
                    callback.txFinished(null, null);
                } catch (Throwable failure) {
                    callback.txFinished(null, failure);
                }
            }
        }.start();
    }

    public <R> void callInTxAsync(final Callable<R> callable, final TxCallback<R> callback) {
        new Thread() {
            @Override
            public void run() {
                try {
                    R result = callInTx(callable);
                    callback.txFinished(result, null);
                } catch (Throwable failure) {
                    callback.txFinished(null, failure);
                }
            }
        }.start();
    }
}
