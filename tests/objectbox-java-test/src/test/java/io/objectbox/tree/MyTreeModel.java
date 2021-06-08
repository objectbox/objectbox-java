
package io.objectbox.tree;

import io.objectbox.BoxStoreBuilder;
import io.objectbox.ModelBuilder;
import io.objectbox.ModelBuilder.EntityBuilder;
import io.objectbox.model.PropertyFlags;
import io.objectbox.model.PropertyType;

/**
 * Tree-relevant model adapted from generated MyObjectBox of integration test project for.
 */
public class MyTreeModel {

    public static BoxStoreBuilder builder() {
        BoxStoreBuilder builder = new BoxStoreBuilder(getModel());
        addTreeEntities(builder);
        return builder;
    }

    public static void addTreeEntities(BoxStoreBuilder builder) {
        builder.entity(DataBranch_.__INSTANCE);
        builder.entity(MetaBranch_.__INSTANCE);
        builder.entity(DataLeaf_.__INSTANCE);
        builder.entity(MetaLeaf_.__INSTANCE);
    }

    private static byte[] getModel() {
        ModelBuilder modelBuilder = new ModelBuilder();
        addTreeModel(modelBuilder);
        modelBuilder.lastEntityId(lastEntityId(),lastEntityUid());
        modelBuilder.lastIndexId(lastIndexId(), lastIndexUid());
        return modelBuilder.build();
    }

    private static void addTreeModel(ModelBuilder modelBuilder) {
        buildEntityDataBranch(modelBuilder);
        buildEntityMetaBranch(modelBuilder);
        buildEntityDataLeaf(modelBuilder);
        buildEntityMetaLeaf(modelBuilder);
    }

    private static void buildEntityDataBranch(ModelBuilder modelBuilder) {
        EntityBuilder entityBuilder = modelBuilder.entity("DataBranch");
        entityBuilder.id(44, 6392934887623369090L).lastPropertyId(4, 5493340424534667127L);
        entityBuilder.flags(io.objectbox.model.EntityFlags.USE_NO_ARG_CONSTRUCTOR);

        entityBuilder.property("id", PropertyType.Long).id(1, 6091686668168828751L)
                .flags(PropertyFlags.ID);
        entityBuilder.property("uid", PropertyType.String).id(2, 618590728777271608L)
                .flags(PropertyFlags.INDEX_HASH | PropertyFlags.UNIQUE | PropertyFlags.UNIQUE_ON_CONFLICT_REPLACE).indexId(101, 2335075400716688517L);
        entityBuilder.property("parentId", "DataBranch", "parent", PropertyType.Relation).id(3, 6451585858539687076L)
                .flags(PropertyFlags.NOT_NULL | PropertyFlags.VIRTUAL | PropertyFlags.INDEXED | PropertyFlags.INDEX_PARTIAL_SKIP_ZERO).indexId(102, 5972614031084366599L);
        entityBuilder.property("metaBranchId", "MetaBranch", "metaBranch", PropertyType.Relation).id(4, 5493340424534667127L)
                .flags(PropertyFlags.NOT_NULL | PropertyFlags.VIRTUAL | PropertyFlags.INDEXED | PropertyFlags.INDEX_PARTIAL_SKIP_ZERO).indexId(103, 8201014180214634794L);


        entityBuilder.entityDone();
    }

    private static void buildEntityMetaBranch(ModelBuilder modelBuilder) {
        EntityBuilder entityBuilder = modelBuilder.entity("MetaBranch");
        entityBuilder.id(45, 1310274875657521237L).lastPropertyId(4, 4595036309339359712L);
        entityBuilder.flags(io.objectbox.model.EntityFlags.USE_NO_ARG_CONSTRUCTOR);

        entityBuilder.property("id", PropertyType.Long).id(1, 1194261531523403259L)
                .flags(PropertyFlags.ID);
        entityBuilder.property("name", PropertyType.String).id(2, 2154298180099032522L);
        entityBuilder.property("description", PropertyType.String).id(3, 7184740729497471956L);
        entityBuilder.property("parentId", "MetaBranch", "parent", PropertyType.Relation).id(4, 4595036309339359712L)
                .flags(PropertyFlags.NOT_NULL | PropertyFlags.VIRTUAL | PropertyFlags.INDEXED | PropertyFlags.INDEX_PARTIAL_SKIP_ZERO).indexId(104, 4816571718244293666L);


        entityBuilder.entityDone();
    }

    private static void buildEntityDataLeaf(ModelBuilder modelBuilder) {
        EntityBuilder entityBuilder = modelBuilder.entity("DataLeaf");
        entityBuilder.id(46, 467861220182153395L).lastPropertyId(7, 4699140392024010132L);
        entityBuilder.flags(io.objectbox.model.EntityFlags.USE_NO_ARG_CONSTRUCTOR);

        entityBuilder.property("id", PropertyType.Long).id(1, 4828688333591497370L)
                .flags(PropertyFlags.ID);
        entityBuilder.property("valueInt", PropertyType.Long).id(2, 5680848072093066354L)
                .flags(PropertyFlags.NOT_NULL);
        entityBuilder.property("valueDouble", PropertyType.Double).id(3, 1807721889883579442L)
                .flags(PropertyFlags.NOT_NULL);
        entityBuilder.property("valueString", PropertyType.String).id(4, 3664314117089332414L);
        entityBuilder.property("valueStrings", PropertyType.StringVector).id(5, 9123492271146452029L);
        entityBuilder.property("dataBranchId", "DataBranch", "dataBranch", PropertyType.Relation).id(6, 8206794170110103204L)
                .flags(PropertyFlags.NOT_NULL | PropertyFlags.VIRTUAL | PropertyFlags.INDEXED | PropertyFlags.INDEX_PARTIAL_SKIP_ZERO).indexId(105, 3957808210294196067L);
        entityBuilder.property("metaLeafId", "MetaLeaf", "metaLeaf", PropertyType.Relation).id(7, 4699140392024010132L)
                .flags(PropertyFlags.NOT_NULL | PropertyFlags.VIRTUAL | PropertyFlags.INDEXED | PropertyFlags.INDEX_PARTIAL_SKIP_ZERO).indexId(106, 977164625385952067L);


        entityBuilder.entityDone();
    }

    private static void buildEntityMetaLeaf(ModelBuilder modelBuilder) {
        EntityBuilder entityBuilder = modelBuilder.entity("MetaLeaf");
        entityBuilder.id(47, 5254271311260799313L).lastPropertyId(10, 8664500113133909445L);
        entityBuilder.flags(io.objectbox.model.EntityFlags.USE_NO_ARG_CONSTRUCTOR);

        entityBuilder.property("id", PropertyType.Long).id(1, 3427494083756650716L)
                .flags(PropertyFlags.ID);
        entityBuilder.property("name", PropertyType.String).id(2, 5713619020507923508L);
        entityBuilder.property("description", PropertyType.String).id(3, 5039668234383376989L);
        entityBuilder.property("flags", PropertyType.Int).id(4, 5257477461658965421L)
                .flags(PropertyFlags.NOT_NULL | PropertyFlags.UNSIGNED);
        entityBuilder.property("valueType", PropertyType.Short).id(5, 7457766704836131009L)
                .flags(PropertyFlags.NOT_NULL | PropertyFlags.UNSIGNED);
        entityBuilder.property("valueEnum", PropertyType.StringVector).id(6, 1440153045496741290L);
        entityBuilder.property("valueUnit", PropertyType.String).id(7, 8462205277100323490L);
        entityBuilder.property("valueMin", PropertyType.String).id(8, 2892243010305059156L);
        entityBuilder.property("valueMax", PropertyType.String).id(9, 2813620764730453795L);
        entityBuilder.property("branchId", "MetaBranch", "branch", PropertyType.Relation).id(10, 8664500113133909445L)
                .flags(PropertyFlags.NOT_NULL | PropertyFlags.VIRTUAL | PropertyFlags.INDEXED | PropertyFlags.INDEX_PARTIAL_SKIP_ZERO).indexId(107, 1337360598786750127L);


        entityBuilder.entityDone();
    }

    // See buildEntityMetaLeaf()
    static int lastEntityId() {
        return 47;
    }

    // See buildEntityMetaLeaf()
    static long lastEntityUid() {
        return 5254271311260799313L;
    }

    // See buildEntityMetaLeaf()
    static int lastIndexId() {
        return 107;
    }

    // See buildEntityMetaLeaf()
    static long lastIndexUid() {
        return 1337360598786750127L;
    }

}
