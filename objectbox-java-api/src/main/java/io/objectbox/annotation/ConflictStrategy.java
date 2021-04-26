package io.objectbox.annotation;

/**
 * Used with {@link Unique} to specify the conflict resolution strategy.
 */
public enum ConflictStrategy {

    /**
     * Throws UniqueViolationException if any property violates a {@link Unique} constraint.
     */
    FAIL,
    /**
     * Any conflicting objects are deleted before the object is inserted.
     */
    REPLACE

}
