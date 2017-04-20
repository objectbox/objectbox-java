
package io.objectbox.relation;

import io.objectbox.Properties;
import io.objectbox.Property;
import io.objectbox.internal.IdGetter;

// THIS CODE IS ADAPTED from generated resources of the test-entity-annotations project

/**
 * Properties for entity "Customer". Can be used for QueryBuilder and for referencing DB names.
 */
public class Customer_ implements Properties {

    public static final String __NAME_IN_DB = "Customer";

    public final static Property id = new Property(0, 1, long.class, "id", true, "_id");
    public final static Property name = new Property(1, 2, String.class, "name");

    public final static Property[] __ALL_PROPERTIES = {
        id,
        name
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

    @Override
    public IdGetter<Customer> getIdGetter() {
        return new IdGetter<Customer>() {
            @Override
            public long getId(Customer object) {
                return object.getId();
            }
        };
    }
}
