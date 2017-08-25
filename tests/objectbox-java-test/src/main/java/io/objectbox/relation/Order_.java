
package io.objectbox.relation;

import javax.annotation.Nullable;

import io.objectbox.BoxStore;
import io.objectbox.Cursor;
import io.objectbox.EntityInfo;
import io.objectbox.Property;
import io.objectbox.Transaction;
import io.objectbox.annotation.apihint.Internal;
import io.objectbox.internal.CursorFactory;
import io.objectbox.internal.IdGetter;
import io.objectbox.internal.ToOneGetter;
import io.objectbox.relation.OrderCursor.Factory;

// THIS CODE IS ADAPTED from generated resources of the test-entity-annotations project

/**
 * Properties for entity "ORDERS". Can be used for QueryBuilder and for referencing DB names.
 */
public class Order_ implements EntityInfo<Order> {


    // Leading underscores for static constants to avoid naming conflicts with property names

    public static final String __ENTITY_NAME = "Order";

    public static final Class<Order> __ENTITY_CLASS = Order.class;

    public static final String __DB_NAME = "ORDERS";

    public static final CursorFactory<Order> __CURSOR_FACTORY = new Factory();

    @Internal
    static final OrderIdGetter __ID_GETTER = new OrderIdGetter();

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

    public final static Order_ __INSTANCE = new Order_();

    @Override
    public String getEntityName() {
        return __ENTITY_NAME;
    }

    @Override
    public Class<Order> getEntityClass() {
        return __ENTITY_CLASS;
    }

    @Override
    public int getEntityId() {
        return 3;
    }

    @Override
    public String getDbName() {
        return __DB_NAME;
    }

    @Override
    public Property[] getAllProperties() {
        return __ALL_PROPERTIES;
    }

    @Override
    public Property getIdProperty() {
        return __ID_PROPERTY;
    }

    @Override
    public IdGetter<Order> getIdGetter() {
        return __ID_GETTER;
    }

    @Override
    public CursorFactory<Order> getCursorFactory() {
        return __CURSOR_FACTORY;
    }

    @Internal
    static final class OrderIdGetter implements IdGetter<Order> {
        public long getId(Order object) {
            return object.getId();
        }
    }

    static final RelationInfo<Customer> customer = new RelationInfo<>(Order_.__INSTANCE, Customer_.__INSTANCE, customerId, new ToOneGetter<Order>() {
        @Override
        public ToOne getToOne(Order object) {
            return object.customer__toOne;
        }
    });

}
