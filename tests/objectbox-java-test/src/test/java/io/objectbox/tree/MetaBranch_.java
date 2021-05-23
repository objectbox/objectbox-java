
package io.objectbox.tree;

import io.objectbox.EntityInfo;
import io.objectbox.annotation.apihint.Internal;
import io.objectbox.internal.CursorFactory;
import io.objectbox.internal.IdGetter;
import io.objectbox.internal.ToOneGetter;
import io.objectbox.relation.RelationInfo;
import io.objectbox.relation.ToOne;
import io.objectbox.tree.MetaBranchCursor.Factory;

// THIS CODE IS GENERATED BY ObjectBox, DO NOT EDIT.

/**
 * Properties for entity "MetaBranch". Can be used for QueryBuilder and for referencing DB names.
 */
public final class MetaBranch_ implements EntityInfo<MetaBranch> {

    // Leading underscores for static constants to avoid naming conflicts with property names

    public static final String __ENTITY_NAME = "MetaBranch";

    public static final int __ENTITY_ID = 45;

    public static final Class<MetaBranch> __ENTITY_CLASS = MetaBranch.class;

    public static final String __DB_NAME = "MetaBranch";

    public static final CursorFactory<MetaBranch> __CURSOR_FACTORY = new Factory();

    @Internal
    static final MetaBranchIdGetter __ID_GETTER = new MetaBranchIdGetter();

    public final static MetaBranch_ __INSTANCE = new MetaBranch_();

    public final static io.objectbox.Property<MetaBranch> id =
        new io.objectbox.Property<>(__INSTANCE, 0, 1, long.class, "id", true, "id");

    public final static io.objectbox.Property<MetaBranch> name =
        new io.objectbox.Property<>(__INSTANCE, 1, 2, String.class, "name");

    public final static io.objectbox.Property<MetaBranch> description =
        new io.objectbox.Property<>(__INSTANCE, 2, 3, String.class, "description");

    public final static io.objectbox.Property<MetaBranch> parentId =
        new io.objectbox.Property<>(__INSTANCE, 3, 4, long.class, "parentId", true);

    @SuppressWarnings("unchecked")
    public final static io.objectbox.Property<MetaBranch>[] __ALL_PROPERTIES = new io.objectbox.Property[]{
        id,
        name,
        description,
        parentId
    };

    public final static io.objectbox.Property<MetaBranch> __ID_PROPERTY = id;

    @Override
    public String getEntityName() {
        return __ENTITY_NAME;
    }

    @Override
    public int getEntityId() {
        return __ENTITY_ID;
    }

    @Override
    public Class<MetaBranch> getEntityClass() {
        return __ENTITY_CLASS;
    }

    @Override
    public String getDbName() {
        return __DB_NAME;
    }

    @Override
    public io.objectbox.Property<MetaBranch>[] getAllProperties() {
        return __ALL_PROPERTIES;
    }

    @Override
    public io.objectbox.Property<MetaBranch> getIdProperty() {
        return __ID_PROPERTY;
    }

    @Override
    public IdGetter<MetaBranch> getIdGetter() {
        return __ID_GETTER;
    }

    @Override
    public CursorFactory<MetaBranch> getCursorFactory() {
        return __CURSOR_FACTORY;
    }

    @Internal
    static final class MetaBranchIdGetter implements IdGetter<MetaBranch> {
        @Override
        public long getId(MetaBranch object) {
            return object.id;
        }
    }

    /** To-one relation "parent" to target entity "MetaBranch". */
    public static final RelationInfo<MetaBranch, MetaBranch> parent =
            new RelationInfo<>(MetaBranch_.__INSTANCE, MetaBranch_.__INSTANCE, parentId, new ToOneGetter<MetaBranch>() {
                @Override
                public ToOne<MetaBranch> getToOne(MetaBranch entity) {
                    return entity.parent;
                }
            });

}
