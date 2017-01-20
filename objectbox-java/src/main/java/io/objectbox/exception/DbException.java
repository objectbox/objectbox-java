package io.objectbox.exception;

public class DbException extends RuntimeException {
    private final int errorCode;

    public DbException(String message) {
        super(message);
        errorCode = 0;
    }

    public DbException(String message, Throwable cause) {
        super(message, cause);
        errorCode = 0;
    }

    public DbException(String message, int errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    @Override
    public String toString() {
        return errorCode == 0 ? super.toString() :
                super.toString() + " (error code " + errorCode + ")";
    }

    /** 0 == no error code available */
    public int getErrorCode() {
        return errorCode;
    }
}
