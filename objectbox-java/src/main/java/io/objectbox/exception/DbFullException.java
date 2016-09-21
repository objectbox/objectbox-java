package io.objectbox.exception;

public class DbFullException extends DbException {
    public DbFullException(String message) {
        super(message);
    }

    public DbFullException(String message, int errorCode) {
        super(message, errorCode);
    }

}
