package io.objectbox.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;


import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * Transient fields are not persisted in the database.
 */
@Retention(CLASS)
@Target(ElementType.FIELD)
public @interface Transient {
}
