package io.objectbox.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines the property serving as the target ID of a ToOne.
 * This allows exposing an explicit property, which may be convenient for other parsers/serializers (e.g. JSON).
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface TargetIdProperty {
    /**
     * Name used in the database.
     */
    String value();

}
