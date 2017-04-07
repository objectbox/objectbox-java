package io.objectbox.annotation;

import io.objectbox.converter.PropertyConverter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies {@link PropertyConverter} for the field to support custom types
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.FIELD)
public @interface Convert {
    /** Converter class */
    Class<? extends PropertyConverter> converter();

    /**
     * Class of the DB type the Java property is converted to/from.
     * This is limited to all java classes which are supported natively by ObjectBox.
     */
    Class dbType();
}
