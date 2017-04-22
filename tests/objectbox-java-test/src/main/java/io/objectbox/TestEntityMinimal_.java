
package io.objectbox;

// Copied from generated tests (& removed some unused Properties)

import io.objectbox.TestEntityMinimalCursor.Factory;
import io.objectbox.annotation.apihint.Internal;
import io.objectbox.internal.CursorFactory;
import io.objectbox.internal.IdGetter;

/**
 * Properties for entity "TestEntity". Can be used for QueryBuilder and for referencing DB names.
 */
public final class TestEntityMinimal_ implements EntityInfo<TestEntityMinimal> {

    // Leading underscores for static constants to avoid naming conflicts with property names

    public static final String __ENTITY_NAME = "TestEntityMinimal";

    public static final Class<TestEntityMinimal> __ENTITY_CLASS = TestEntityMinimal.class;

    public static final String __DB_NAME = "TestEntityMinimal";

    public static final CursorFactory<TestEntityMinimal> __CURSOR_FACTORY = new Factory();

    @Internal
    static final IdGetter<TestEntityMinimal> __ID_GETTER = new IdGetter<TestEntityMinimal>() {
        @Override
        public long getId(TestEntityMinimal object) {
            return object.getId();
        }
    };

    private static int ID;

    public final static Property id = new Property(ID++, ID, long.class, "id", true, "id");
    public final static Property text = new Property(ID++, ID, String.class, "text", false, "text");

    public final static Property[] __ALL_PROPERTIES = {
            id,
            text,
    };

    public final static Property __ID_PROPERTY = id;

    @Override
    public String getEntityName() {
        return __ENTITY_NAME;
    }

    @Override
    public Class<TestEntityMinimal> getEntityClass() {
        return __ENTITY_CLASS;
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
    public IdGetter<TestEntityMinimal> getIdGetter() {
        return __ID_GETTER;
    }

    @Override
    public CursorFactory<TestEntityMinimal> getCursorFactory() {
        return __CURSOR_FACTORY;
    }

    @Internal
    static final class TestEntityIdGetter implements IdGetter<TestEntity> {
        public long getId(TestEntity object) {
            return object.getId();
        }
    }

}
