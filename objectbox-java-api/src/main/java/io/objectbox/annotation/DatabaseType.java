package io.objectbox.annotation;

/**
 * Use with {@link Type @Type} to override how a property value is stored and interpreted in the database.
 */
public enum DatabaseType {

    /**
     * Use with 64-bit long properties to store them as high precision time
     * representing nanoseconds since 1970-01-01 (unix epoch).
     * <p>
     * By default, a 64-bit long value is interpreted as time in milliseconds (a Date).
     */
    DateNano

}
