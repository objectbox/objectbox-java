package io.objectbox.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines the Java code of the default value to use for a property, when getting an existing entity and the database
 * value for the property is null.
 * <p>
 * Currently only {@code @DefaultValue("")} is supported.
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.FIELD})
public @interface DefaultValue {
    String value();
}
