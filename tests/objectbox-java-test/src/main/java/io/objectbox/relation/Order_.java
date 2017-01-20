
package io.objectbox.relation;

import io.objectbox.Properties;
import io.objectbox.Property;

// THIS CODE IS ADAPTED from generated resources of the test-entity-annotations project

/**
 * Properties for entity "ORDERS". Can be used for QueryBuilder and for referencing DB names.
 */
public class Order_ implements Properties {

    public static final String __NAME_IN_DB = "ORDERS";

    public final static Property id = new Property(0, 1, long.class, "id", true, "_id");
    public final static Property date = new Property(1, 2, java.util.Date.class, "date");
    public final static Property customerId = new Property(2, 3, long.class, "customerId");
    public final static Property text = new Property(3, 4, String.class, "text");

    public final static Property[] __ALL_PROPERTIES = {
        id,
        date,
        customerId,
        text
    };

    public final static Property __ID_PROPERTY = id;

    @Override
    public Property[] getAllProperties() {
        return __ALL_PROPERTIES;
    }

    @Override
    public Property getIdProperty() {
        return __ID_PROPERTY;
    }

    @Override
    public String getDbName() {
        return __NAME_IN_DB;
    }

}
