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

import io.objectbox.BoxStore;
import io.objectbox.BoxStoreBuilder;
import io.objectbox.ModelBuilder;
import io.objectbox.ModelBuilder.EntityBuilder;
import io.objectbox.model.PropertyFlags;
import io.objectbox.model.PropertyType;


// THIS CODE IS ADAPTED from generated resources of the test-entity-annotations project
/**
 * Starting point for working with your ObjectBox. All boxes are set up for your objects here.
 * <p>
 * First steps (Android): get a builder using {@link #builder()}, call {@link BoxStoreBuilder#androidContext(Object)},
 * and {@link BoxStoreBuilder#build()} to get a {@link BoxStore} to work with.
 */
public class MyObjectBox {

    public static BoxStoreBuilder builder() {
        BoxStoreBuilder builder = new BoxStoreBuilder(getModel());
        builder.entity(new Customer_());
        builder.entity(new Order_());
        return builder;
    }

    private static byte[] getModel() {
        ModelBuilder modelBuilder = new ModelBuilder();
        modelBuilder.lastEntityId(4, 5318696586219463633L);
        modelBuilder.lastIndexId(2, 8919874872236271392L);
        modelBuilder.lastRelationId(1, 8943758920347589435L);

        EntityBuilder entityBuilder;

        entityBuilder = modelBuilder.entity("Customer");
        entityBuilder.id(1, 8247662514375611729L).lastPropertyId(2, 7412962174183812632L);
        entityBuilder.property("_id", PropertyType.Long).id(1, 1888039726372206411L)
                .flags(PropertyFlags.ID | PropertyFlags.ID_SELF_ASSIGNABLE);
        entityBuilder.property("name", PropertyType.String).id(2, 7412962174183812632L)
                .flags(PropertyFlags.INDEXED).indexId(1, 5782921847050580892L);
        entityBuilder.relation("ordersStandalone", 1, 8943758920347589435L, 3, 6367118380491771428L);
        entityBuilder.entityDone();


        entityBuilder = modelBuilder.entity("ORDERS");
//        entityBuilder = modelBuilder.entity("Order");
        entityBuilder.id(3, 6367118380491771428L).lastPropertyId(4, 1061627027714085430L);
        entityBuilder.property("_id", PropertyType.Long).id(1, 7221142423462017794L)
                .flags(PropertyFlags.ID | PropertyFlags.ID_SELF_ASSIGNABLE);
        entityBuilder.property("date", PropertyType.Date).id(2, 2751944693239151491L);
        entityBuilder.property("customerId", "Customer", PropertyType.Relation).id(3, 7825181002293047239L)
                .flags(PropertyFlags.NOT_NULL | PropertyFlags.INDEXED | PropertyFlags.INDEX_PARTIAL_SKIP_ZERO).indexId(2, 8919874872236271392L);
        entityBuilder.property("text", PropertyType.String).id(4, 1061627027714085430L);
        entityBuilder.entityDone();

        return modelBuilder.build();
    }

}
