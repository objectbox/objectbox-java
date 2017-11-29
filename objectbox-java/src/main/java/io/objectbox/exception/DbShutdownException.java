package io.objectbox.exception;

/**
 * Thrown when an error occurred that requires the DB to shutdown.
 * This may be an I/O error for example.
 * Regular operations won't be possible anymore.
 * To handle that situation you could exit the app or try to reopen the store.
 */
public class DbShutdownException extends DbException {
    public DbShutdownException(String message) {
        super(message);
    }

    public DbShutdownException(String message, int errorCode) {
        super(message, errorCode);
    }

}
