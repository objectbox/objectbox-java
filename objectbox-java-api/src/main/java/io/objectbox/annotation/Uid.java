package io.objectbox.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * UIDs identify entities (and properties) uniquely in the meta object model file (objectbox-model/default.json).
 * With UIDs you can map entities to their meta model representation in a stable way without its name.
 * Once a UID is set, you can rename the entity as often as you like - ObjectBox keeps track of it automatically.
 * Thus, it is advisable to lookup the UID in objectbox-model/default.json and use it here before renaming a entity.
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface Uid {
    long value() default 0;
}
