package io.objectbox.annotation;

/**
 * Used with {@link Unique} to specify the conflict resolution strategy.
 */
public enum ConflictStrategy {

    /**
     * Default. Throws UniqueViolationException if any property violates a {@link Unique} constraint.
     */
    FAIL,
    /**
     * Ignore the offending object (the existing object is not changed). If there are multiple unique properties in an
     * entity, this strategy is evaluated first: if the property conflicts, no other properties will be checked for
     * conflicts.
     */
    IGNORE,
    /**
     * The offending object replaces the existing object (deletes it). If there are multiple properties using this
     * strategy, a single put can potentially replace (delete) multiple existing objects.
     */
    REPLACE,
    /**
     * The offending object overwrites the existing object while keeping its ID. All relations pointing to the existing
     * entity are preserved. This is useful for a "secondary" ID, such as a string "ID". Within an entity, this strategy
     * may be used once only (update target would be ambiguous otherwise).
     */
    UPDATE

}
