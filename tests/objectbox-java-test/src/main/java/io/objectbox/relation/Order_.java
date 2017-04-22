
package io.objectbox.relation;

import javax.annotation.Nullable;

import io.objectbox.BoxStore;
import io.objectbox.Cursor;
import io.objectbox.Properties;
import io.objectbox.Property;
import io.objectbox.Transaction;
import io.objectbox.internal.CursorFactory;
import io.objectbox.internal.IdGetter;

// THIS CODE IS ADAPTED from generated resources of the test-entity-annotations project

/**
 * Properties for entity "ORDERS". Can be used for QueryBuilder and for referencing DB names.
 */
public class Order_ implements Properties<Order> {

    public static final String __NAME_IN_DB = "ORDERS";
//    public static final String __NAME_IN_DB = "Order";

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
    public String getEntityName() {
        return "Order";
    }

    @Override
    public String getDbName() {
        return __NAME_IN_DB;
    }

    @Override
    public Class<Order> getEntityClass() {
        return Order.class;
    }

    @Override
    public  IdGetter<Order> getIdGetter() {
        return new IdGetter<Order>() {
            @Override
            public long getId(Order object) {
                return object.getId();
            }
        };
    }

    @Override
    public CursorFactory<Order> getCursorFactory() {
        return new CursorFactory<Order>() {
            @Override
            public Cursor<Order> createCursor(Transaction tx, long cursorHandle, @Nullable BoxStore boxStoreForEntities) {
                return new OrderCursor(tx, cursorHandle, boxStoreForEntities);
            }
        };
    }
}
