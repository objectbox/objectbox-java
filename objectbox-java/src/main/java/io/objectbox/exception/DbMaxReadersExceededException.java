package io.objectbox.exception;

import io.objectbox.BoxStore;

/**
 * Thrown when the maximum of readers (read transactions) was exceeded.
 * Verify that you run a reasonable amount of threads only.
 * <p>
 * If you intend to work with a very high number of threads (>100), consider increasing the number of maximum readers
 * using {@link io.objectbox.BoxStoreBuilder#maxReaders(int)}.
 * <p>
 * For debugging issues related to this exception, check {@link BoxStore#diagnose()}.
 */
public class DbMaxReadersExceededException extends DbException {
    public DbMaxReadersExceededException(String message) {
        super(message);
    }

    public DbMaxReadersExceededException(String message, int errorCode) {
        super(message, errorCode);
    }
}
