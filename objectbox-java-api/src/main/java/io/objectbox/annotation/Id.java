package io.objectbox.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks field is the primary key of the entity's table
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.FIELD)
public @interface Id {
    /**
     * Specifies that id should increase monotonic without reusing IDs. This decreases performance a little bit for
     * putting new objects (inserts).
     */
    boolean monotonic() default false;
}
