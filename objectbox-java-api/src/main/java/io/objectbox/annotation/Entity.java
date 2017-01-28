package io.objectbox.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for entities
 * ObjectBox only persist objects of classes which are marked with this annotation
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface Entity {

    /**
     * Specifies the name on the DB side (e.g. table name) this entity maps to.
     * By default, the name is based on the entities class name.
     * <p>
     * Note: if you intent to rename an entity, consider using uid instead.
     */
    String nameInDb() default "";

//    /**
//     * Advanced flag to disable table creation in the database (when set to false). This can be used to create partial
//     * entities, which may use only a sub set of properties. Be aware however that ObjectBox does not sync multiple
//     * entities, e.g. in caches.
//     */
//    boolean createInDb() default true;
//
//    /**
//     * Specifies schema name for the entity: ObjectBox can generate independent sets of classes for each schema.
//     * Entities which belong to different schemas should <strong>not</strong> have relations.
//     */
//    String schema() default "default";
//
//    /**
//     * Whether update/delete/refresh methods should be generated.
//     * If entity has defined {@link Relation}, then it is active independently from this value
//     */
//    boolean active() default false;
//
    /**
     * Whether an all properties constructor should be generated. A no-args constructor is always required.
     */
    boolean generateConstructors() default true;

    /**
     * Whether getters and setters for properties should be generated if missing.
     */
    boolean generateGettersSetters() default false;
//
//    /**
//     * Define a protobuf class of this entity to create an additional, special DAO for.
//     */
//    Class protobuf() default void.class;

    /**
     * UIDs identify entities (and properties) uniquely in the meta object model file (objectmodel.json).
     * With UIDs you can map entities to their meta model representation in a stable way without its name.
     * Once a UID is set, you can rename the entity as often as you like - ObjectBox keeps track of it automatically.
     * Thus, it is advisable to lookup the UID in objectmodel.json and use it here before renaming a entity.
     */
    long uid() default 0;

}
