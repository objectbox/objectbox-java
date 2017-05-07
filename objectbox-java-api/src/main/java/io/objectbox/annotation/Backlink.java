package io.objectbox.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.objectbox.annotation.apihint.Beta;

/**
 * Defines a backlink relation, which is based on another relation reversing the direction.
 * <p>
 * Example: one "Order" references one "Customer" (to-one relation).
 * The backlink to this is a to-many in the reverse direction: one "Customer" has a number of "Order"s.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.FIELD)
@Beta
public @interface Backlink {
    /**
     * Name of the relation the backlink should be based on (e.g. name of a ToOne property in the target entity).
     * Can be left empty if there is just a single relation from the target to the source entity.
     */
    String to() default "";
}
