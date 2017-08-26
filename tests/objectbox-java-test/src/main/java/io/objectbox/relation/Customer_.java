
package io.objectbox.relation;

import io.objectbox.EntityInfo;
import io.objectbox.Property;
import io.objectbox.annotation.apihint.Internal;
import io.objectbox.internal.CursorFactory;
import io.objectbox.internal.IdGetter;
import io.objectbox.internal.ToManyGetter;
import io.objectbox.internal.ToOneGetter;
import io.objectbox.relation.CustomerCursor.Factory;

// THIS CODE IS ADAPTED from generated resources of the test-entity-annotations project

/**
 * Properties for entity "Customer". Can be used for QueryBuilder and for referencing DB names.
 */
public class Customer_ implements EntityInfo<Customer> {

    // Leading underscores for static constants to avoid naming conflicts with property names

    public static final String __ENTITY_NAME = "Customer";

    public static final Class<Customer> __ENTITY_CLASS = Customer.class;

    public static final String __DB_NAME = "Customer";

    public static final CursorFactory<Customer> __CURSOR_FACTORY = new Factory();

    @Internal
    static final CustomerIdGetter __ID_GETTER = new CustomerIdGetter();

    public final static Property id = new Property(0, 1, long.class, "id", true, "_id");
    public final static Property name = new Property(1, 2, String.class, "name");

    public final static Property[] __ALL_PROPERTIES = {
            id,
            name
    };

    public final static Property __ID_PROPERTY = id;

    public final static Customer_ __INSTANCE = new Customer_();

    @Override
    public String getEntityName() {
        return __ENTITY_NAME;
    }

    @Override
    public Class<Customer> getEntityClass() {
        return __ENTITY_CLASS;
    }

    @Override
    public int getEntityId() {
        return 1;
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
    public IdGetter<Customer> getIdGetter() {
        return __ID_GETTER;
    }

    @Override
    public CursorFactory<Customer> getCursorFactory() {
        return __CURSOR_FACTORY;
    }

    @Internal
    static final class CustomerIdGetter implements IdGetter<Customer> {
        public long getId(Customer object) {
            return object.getId();
        }
    }

    static final RelationInfo<Order> orders =
            new RelationInfo<>(Customer_.__INSTANCE, Order_.__INSTANCE, new ToManyGetter<Customer>() {
                @Override
                public ToMany<Order> getToMany(Customer customer) {
                    return (ToMany<Order>) customer.getOrders();
                }
            }, Order_.customerId, new ToOneGetter<Order>() {
                @Override
                public ToOne<Customer> getToOne(Order order) {
                    return order.customer__toOne;
                }
            });

    static final RelationInfo<Order> ordersStandalone =
            new RelationInfo<>(Customer_.__INSTANCE, Order_.__INSTANCE, new ToManyGetter<Customer>() {
                @Override
                public ToMany<Order> getToMany(Customer customer) {
                    return (ToMany<Order>) customer.getOrders();
                }
            }, 1);

}
