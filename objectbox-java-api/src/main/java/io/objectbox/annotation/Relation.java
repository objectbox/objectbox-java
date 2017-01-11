package io.objectbox.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.objectbox.annotation.apihint.Beta;

/**
 * Marks the property (type must be an entity or a List of entities) as a relation (optional?).
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.FIELD)
@Beta
public @interface Relation {
    /**
     * Name of the property holding the id (key) as a base for this relation.
     * <p>
     * For to-one relations, the id property must reside in the same entity.
     * <p>
     * For to-many relations, the id must reside in the target entity as part of a to-one @Relation. This implies that
     * to-many relations require a to-one relation in the target entity pointing to this entity.
     * <p>
     */
    String idProperty() default "";
}
