
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

    public final static TestEntityMinimal_ __INSTANCE = new TestEntityMinimal_();

    public final static Property<TestEntityMinimal> id = new Property<>(__INSTANCE, 0, 1, long.class, "id", true, "id");
    public final static Property<TestEntityMinimal> text = new Property<>(__INSTANCE, 1, 2, String.class, "text", false, "text");

    @SuppressWarnings("unchecked")
    public final static Property<TestEntityMinimal>[] __ALL_PROPERTIES = new Property[]{
            id,
            text,
    };

    public final static Property<TestEntityMinimal> __ID_PROPERTY = id;

    @Override
    public String getEntityName() {
        return __ENTITY_NAME;
    }

    @Override
    public Class<TestEntityMinimal> getEntityClass() {
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
    public Property<TestEntityMinimal>[] getAllProperties() {
        return __ALL_PROPERTIES;
    }

    @Override
    public Property<TestEntityMinimal> getIdProperty() {
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
