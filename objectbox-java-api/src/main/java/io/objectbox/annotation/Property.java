package io.objectbox.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Optional: configures the mapped column for a persistent field.
 * This annotation is also applicable with @ToOne without additional foreign key property
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.FIELD)
public @interface Property {
    /**
     * Name of the database column for this property. Default is field name.
     * <p>
     * Note: if you intent to rename a property, consider using refId instead.
     */
    String nameInDb() default "";


    /**
     * RefIDs identify properties (and entities) uniquely in the meta object model file (objectmodel.json).
     * With refIDs you can map properties to their meta model representation in a stable way without its name.
     * Once a refID is set, you can rename the properties as often as you like - ObjectBox keeps track of it automatically.
     * Thus, it is advisable to lookup the refID in objectmodel.json and use it here before renaming a property.
     */
    long refId() default 0;
}
