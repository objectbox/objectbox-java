package io.objectbox.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks field is the primary key of the entity's table
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.FIELD)
public @interface Id {
    /**
     * Specifies that id should increase monotonic without reusing IDs. This decreases performance a little bit for
     * putting new objects (inserts) because the state needs to be persisted. Gaps between two IDs may still occur,
     * e.g. if inserts are rollbacked.
     */
    boolean monotonic() default false;

    /**
     * Allows IDs to be assigned by the developer. This may make sense for using IDs originating somewhere else, e.g.
     * from the server.
     */
    boolean assignable() default false;
}
