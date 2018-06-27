
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

import io.objectbox.TestEntityCursor.Factory;
import io.objectbox.annotation.apihint.Internal;
import io.objectbox.internal.CursorFactory;
import io.objectbox.internal.IdGetter;

/**
 * Properties for entity "TestEntity". Can be used for QueryBuilder and for referencing DB names.
 */
public final class TestEntity_ implements EntityInfo<TestEntity> {

    // Leading underscores for static constants to avoid naming conflicts with property names

    public static final String __ENTITY_NAME = "TestEntity";

    public static final Class<TestEntity> __ENTITY_CLASS = TestEntity.class;

    public static final String __DB_NAME = "TestEntity";

    public static final CursorFactory<TestEntity> __CURSOR_FACTORY = new Factory();

    @Internal
    static final TestEntityIdGetter __ID_GETTER = new TestEntityIdGetter();

    public final static TestEntity_ __INSTANCE = new TestEntity_();

    private static int ID;

    public final static Property<TestEntity> id = new Property(__INSTANCE, ID++, ID, long.class, "id", true, "id");
    public final static Property<TestEntity> simpleBoolean = new Property(__INSTANCE, ID++, ID, boolean.class, "simpleBoolean", false, "simpleBoolean");
    public final static Property<TestEntity> simpleByte = new Property(__INSTANCE, ID++, ID, byte.class, "simpleByte", false, "simpleByte");
    public final static Property<TestEntity> simpleShort = new Property(__INSTANCE, ID++, ID, short.class, "simpleShort", false, "simpleShort");
    public final static Property<TestEntity> simpleInt = new Property(__INSTANCE, ID++, ID, int.class, "simpleInt", false, "simpleInt");
    public final static Property<TestEntity> simpleLong = new Property(__INSTANCE, ID++, ID, long.class, "simpleLong", false, "simpleLong");
    public final static Property<TestEntity> simpleFloat = new Property(__INSTANCE, ID++, ID, float.class, "simpleFloat", false, "simpleFloat");
    public final static Property<TestEntity> simpleDouble = new Property(__INSTANCE, ID++, ID, double.class, "simpleDouble", false, "simpleDouble");
    public final static Property<TestEntity> simpleString = new Property(__INSTANCE, ID++, ID, String.class, "simpleString", false, "simpleString");
    public final static Property<TestEntity> simpleByteArray = new Property(__INSTANCE, ID++, ID, byte[].class, "simpleByteArray", false, "simpleByteArray");

    public final static Property<TestEntity>[] __ALL_PROPERTIES = new Property[]{
            id,
            simpleInt,
            simpleShort,
            simpleLong,
            simpleString,
            simpleFloat,
            simpleBoolean,
            simpleByteArray
    };

    public final static Property __ID_PROPERTY = id;

    @Override
    public String getEntityName() {
        return __ENTITY_NAME;
    }

    @Override
    public Class<TestEntity> getEntityClass() {
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
    public IdGetter<TestEntity> getIdGetter() {
        return __ID_GETTER;
    }

    @Override
    public CursorFactory<TestEntity> getCursorFactory() {
        return __CURSOR_FACTORY;
    }

    @Internal
    static final class TestEntityIdGetter implements IdGetter<TestEntity> {
        public long getId(TestEntity object) {
            return object.getId();
        }
    }

}
