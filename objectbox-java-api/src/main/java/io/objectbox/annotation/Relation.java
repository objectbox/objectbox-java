package io.objectbox.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.objectbox.annotation.apihint.Beta;
import io.objectbox.annotation.apihint.Temporary;

/**
 * Optional annotation for ToOnes to specify a property serving as an ID to the target.
 * Note: this annotation will likely be renamed/changed in the next version.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.FIELD)
@Beta
@Temporary
@Deprecated
public @interface Relation {
    /**
     * Name of the property (in the source entity) holding the id (key) as a base for this to-one relation.
     */
    String idProperty() default "";
}
