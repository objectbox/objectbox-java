
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

import io.objectbox.TestEntityCursor.Factory;
import io.objectbox.annotation.apihint.Internal;
import io.objectbox.converter.StringFlexMapConverter;
import io.objectbox.internal.CursorFactory;
import io.objectbox.internal.IdGetter;

import java.util.Map;

// NOTE: Copied from a plugin project (& removed some unused Properties).
// THIS CODE IS GENERATED BY ObjectBox, DO NOT EDIT.

/**
 * Properties for entity "TestEntity". Can be used for QueryBuilder and for referencing DB names.
 */
public final class TestEntity_ implements EntityInfo<TestEntity> {

    // Leading underscores for static constants to avoid naming conflicts with property names

    public static final String __ENTITY_NAME = "TestEntity";

    public static final int __ENTITY_ID = 1;

    public static final Class<TestEntity> __ENTITY_CLASS = TestEntity.class;

    public static final String __DB_NAME = "TestEntity";

    public static final CursorFactory<TestEntity> __CURSOR_FACTORY = new Factory();

    @Internal
    static final TestEntityIdGetter __ID_GETTER = new TestEntityIdGetter();

    public final static TestEntity_ __INSTANCE = new TestEntity_();

    public final static io.objectbox.Property<TestEntity> id =
            new io.objectbox.Property<>(__INSTANCE, 0, 1, long.class, "id", true, "id");

    public final static io.objectbox.Property<TestEntity> simpleBoolean =
            new io.objectbox.Property<>(__INSTANCE, 1, 2, boolean.class, "simpleBoolean");

    public final static io.objectbox.Property<TestEntity> simpleByte =
            new io.objectbox.Property<>(__INSTANCE, 2, 3, byte.class, "simpleByte");

    public final static io.objectbox.Property<TestEntity> simpleShort =
            new io.objectbox.Property<>(__INSTANCE, 3, 4, short.class, "simpleShort");

    public final static io.objectbox.Property<TestEntity> simpleInt =
            new io.objectbox.Property<>(__INSTANCE, 4, 5, int.class, "simpleInt");

    public final static io.objectbox.Property<TestEntity> simpleLong =
            new io.objectbox.Property<>(__INSTANCE, 5, 6, long.class, "simpleLong");

    public final static io.objectbox.Property<TestEntity> simpleFloat =
            new io.objectbox.Property<>(__INSTANCE, 6, 7, float.class, "simpleFloat");

    public final static io.objectbox.Property<TestEntity> simpleDouble =
            new io.objectbox.Property<>(__INSTANCE, 7, 8, double.class, "simpleDouble");

    public final static io.objectbox.Property<TestEntity> simpleString =
            new io.objectbox.Property<>(__INSTANCE, 8, 9, String.class, "simpleString");

    public final static io.objectbox.Property<TestEntity> simpleByteArray =
            new io.objectbox.Property<>(__INSTANCE, 9, 10, byte[].class, "simpleByteArray");

    public final static io.objectbox.Property<TestEntity> simpleStringArray =
            new io.objectbox.Property<>(__INSTANCE, 10, 11, String[].class, "simpleStringArray", false, "simpleStringArray");

    public final static io.objectbox.Property<TestEntity> simpleStringList =
            new io.objectbox.Property<>(__INSTANCE, 11, 15, java.util.List.class, "simpleStringList");

    public final static io.objectbox.Property<TestEntity> simpleShortU =
            new io.objectbox.Property<>(__INSTANCE, 12, 12, short.class, "simpleShortU");

    public final static io.objectbox.Property<TestEntity> simpleIntU =
            new io.objectbox.Property<>(__INSTANCE, 13, 13, int.class, "simpleIntU");

    public final static io.objectbox.Property<TestEntity> simpleLongU =
            new io.objectbox.Property<>(__INSTANCE, 14, 14, long.class, "simpleLongU");

    public final static io.objectbox.Property<TestEntity> stringObjectMap =
            new io.objectbox.Property<>(__INSTANCE, 15, 16, byte[].class, "stringObjectMap", false, "stringObjectMap", StringFlexMapConverter.class, Map.class);

    @SuppressWarnings("unchecked")
    public final static io.objectbox.Property<TestEntity>[] __ALL_PROPERTIES = new io.objectbox.Property[]{
            id,
            simpleBoolean,
            simpleByte,
            simpleShort,
            simpleInt,
            simpleLong,
            simpleFloat,
            simpleDouble,
            simpleString,
            simpleByteArray,
            simpleStringArray,
            simpleStringList,
            simpleShortU,
            simpleIntU,
            simpleLongU,
            stringObjectMap
    };

    public final static io.objectbox.Property<TestEntity> __ID_PROPERTY = id;

    @Override
    public String getEntityName() {
        return __ENTITY_NAME;
    }

    @Override
    public int getEntityId() {
        return __ENTITY_ID;
    }

    @Override
    public Class<TestEntity> getEntityClass() {
        return __ENTITY_CLASS;
    }

    @Override
    public String getDbName() {
        return __DB_NAME;
    }

    @Override
    public io.objectbox.Property<TestEntity>[] getAllProperties() {
        return __ALL_PROPERTIES;
    }

    @Override
    public io.objectbox.Property<TestEntity> getIdProperty() {
        return __ID_PROPERTY;
    }

    @Override
    public IdGetter<TestEntity> getIdGetter() {
        return __ID_GETTER;
    }

    @Override
    public CursorFactory<TestEntity> getCursorFactory() {
        return __CURSOR_FACTORY;
    }

    @Internal
    static final class TestEntityIdGetter implements IdGetter<TestEntity> {
        @Override
        public long getId(TestEntity object) {
            return object.getId();
        }
    }

}
