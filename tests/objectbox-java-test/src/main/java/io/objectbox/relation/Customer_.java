
package io.objectbox.relation;

import javax.annotation.Nullable;

import io.objectbox.BoxStore;
import io.objectbox.Cursor;
import io.objectbox.EntityInfo;
import io.objectbox.Property;
import io.objectbox.Transaction;
import io.objectbox.internal.CursorFactory;
import io.objectbox.internal.IdGetter;

// THIS CODE IS ADAPTED from generated resources of the test-entity-annotations project

/**
 * Properties for entity "Customer". Can be used for QueryBuilder and for referencing DB names.
 */
public class Customer_ implements EntityInfo<Customer> {

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
    public String getEntityName() {
        return "Customer";
    }

    @Override
    public String getDbName() {
        return __NAME_IN_DB;
    }

    @Override
    public Class<Customer> getEntityClass() {
        return Customer.class;
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

    @Override
    public CursorFactory<Customer> getCursorFactory() {
        return new CursorFactory<Customer>() {
            @Override
            public Cursor<Customer> createCursor(Transaction tx, long cursorHandle, @Nullable BoxStore boxStoreForEntities) {
                return new CustomerCursor(tx, cursorHandle, boxStoreForEntities);
            }
        };
    }
}
