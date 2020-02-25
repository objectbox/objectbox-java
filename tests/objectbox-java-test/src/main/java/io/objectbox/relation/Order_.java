
/*
 * Copyright 2017 ObjectBox Ltd. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.objectbox.relation;

import io.objectbox.EntityInfo;
import io.objectbox.Property;
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

    public final static Order_ __INSTANCE = new Order_();

    public final static Property<Order> id = new Property<>(__INSTANCE, 0, 1, long.class, "id", true, "_id");
    public final static Property<Order> date = new Property<>(__INSTANCE, 1, 2, java.util.Date.class, "date");
    public final static Property<Order> customerId = new Property<>(__INSTANCE, 2, 3, long.class, "customerId");
    public final static Property<Order> text = new Property<>(__INSTANCE, 3, 4, String.class, "text");

    @SuppressWarnings("unchecked")
    public final static Property<Order>[] __ALL_PROPERTIES = new Property[]{
            id,
            date,
            customerId,
            text
    };

    public final static Property<Order> __ID_PROPERTY = id;

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
    public Property<Order>[] getAllProperties() {
        return __ALL_PROPERTIES;
    }

    @Override
    public Property<Order> getIdProperty() {
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

    static final RelationInfo<Order, Customer> customer = new RelationInfo<>(Order_.__INSTANCE, Customer_.__INSTANCE, customerId, new ToOneGetter<Order>() {
        @Override
        public ToOne<Customer> getToOne(Order object) {
            return object.customer__toOne;
        }
    });

}
