package io.objectbox.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as an ObjectBox Entity super class.
 * ObjectBox will store properties of this class as part of an Entity that inherits from this class.
 * See <a href="https://docs.objectbox.io/advanced/entity-inheritance">the Entity Inheritance documentation</a> for details.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface BaseEntity {
}
