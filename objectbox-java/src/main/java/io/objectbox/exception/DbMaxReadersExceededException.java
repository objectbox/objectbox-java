package io.objectbox.exception;

/**
 * Thrown when the maximum of readers (read transactions) was exceeded.
 * Verify that you run a reasonable amount of threads only.
 * If you intend to work with a very high number of threads (>100), consider increasing the number of maximum readers
 * using {@link io.objectbox.BoxStoreBuilder#maxReaders(int)}.
 */
public class DbMaxReadersExceededException extends DbException {
    public DbMaxReadersExceededException(String message) {
        super(message);
    }

}
