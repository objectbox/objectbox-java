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
        builder.entity("Customer", Customer.class, CustomerCursor.class, new Customer_());
        builder.entity("ORDERS", Order.class, OrderCursor.class, new Order_());
        return builder;
    }

    private static byte[] getModel() {
        ModelBuilder modelBuilder = new ModelBuilder();
        modelBuilder.lastEntityId(4);
        modelBuilder.lastIndexId(2);

        EntityBuilder entityBuilder;

        entityBuilder = modelBuilder.entity("Customer");
        entityBuilder.id(1).uid(3625336331812221361L).lastPropertyId(2);
        entityBuilder.property("_id", PropertyType.Long).id(1).uid(1582995887554488290L)
            .flags(PropertyFlags.ID | PropertyFlags.NOT_NULL);
        entityBuilder.property("name", PropertyType.String).id(2).uid(3080561794084640807L)
            .flags(PropertyFlags.INDEXED).indexId(1);
        entityBuilder.entityDone();


        entityBuilder = modelBuilder.entity("ORDERS");
        entityBuilder.id(3).uid(4761318278698541254L).lastPropertyId(4);
        entityBuilder.property("_id", PropertyType.Long).id(1).uid(4065349512068827171L)
            .flags(PropertyFlags.ID | PropertyFlags.ID_SELF_ASSIGNABLE | PropertyFlags.NOT_NULL);
        entityBuilder.property("date", PropertyType.Date).id(2).uid(1517800508838480650L);
        entityBuilder.property("customerId", "Customer", PropertyType.Relation).id(3).uid(7549816757665526666L)
            .flags(PropertyFlags.NOT_NULL | PropertyFlags.INDEXED | PropertyFlags.INDEX_PARTIAL_SKIP_ZERO).indexId(2);
        entityBuilder.property("text", PropertyType.String).id(4).uid(4188485512096015343L);
        entityBuilder.entityDone();

        return modelBuilder.build();
    }

}
