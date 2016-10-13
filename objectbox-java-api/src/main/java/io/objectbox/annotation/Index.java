package io.objectbox.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies that the property should be indexed, which is highly recommended if you do queries using this property.
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.FIELD)
public @interface Index {
//    /**
//     * Whether the unique constraint should be created with base on this index
//     */
//    boolean unique() default false;
}
