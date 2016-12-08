package io.objectbox.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines *-to-N relation
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.FIELD)
/** TODO public */ @interface ToMany {
    /**
     * Name of the property inside the target entity which holds id of the source (current) entity
     * Required unless no {@link JoinProperty} or {@link JoinEntity} is specified
     */
    String referencedJoinProperty() default "";

    /**
     * Array of matching source to target properties
     * Required unless {@link #referencedJoinProperty()} or {@link JoinEntity} is specified
     */
    JoinProperty[] joinProperties() default {};
}
