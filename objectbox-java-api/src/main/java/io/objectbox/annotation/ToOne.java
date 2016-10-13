package io.objectbox.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines *-to-1 relation with base on existing property
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.FIELD)
/** TODO public */ @interface ToOne {
    /**
     * Name of the property inside the current entity which holds the key of related entity.
     * If this parameter is absent, then an additional column is automatically created to hold the key.
     */
    String joinProperty() default "";
}
