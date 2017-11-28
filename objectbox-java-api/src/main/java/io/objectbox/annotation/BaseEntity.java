package io.objectbox.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for an entity base class.
 * ObjectBox will include properties of an entity super class marked with this annotation.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface BaseEntity {
}
