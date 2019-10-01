package io.objectbox.annotation;

/**
 * Use with {@link Type @Type} to specify how a property value is stored in the database.
 * <p>
 * This is e.g. useful for integer types that can mean different things depending on interpretation.
 * For example a 64-bit long value might be interpreted as time in milliseconds, or as time in nanoseconds.
 */
public enum DatabaseType {

    /**
     * High precision time stored as a 64-bit long representing nanoseconds since 1970-01-01 (unix epoch).
     */
    DateNano

}
