package io.objectbox.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Allows to specify a simple name mapping for entities and properties.
 * If names have diverged on the Java side (vs. the DB), you can specify the name used in the database here.
 * This allows simple renames in Java. For more advanced renames you should consider @{@link Uid} instead.
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface NameInDb {
    /**
     * Name used in the database.
     */
    String value();

}
