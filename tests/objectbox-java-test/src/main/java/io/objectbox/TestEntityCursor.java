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


import io.objectbox.annotation.apihint.Internal;
import io.objectbox.internal.CursorFactory;

// THIS CODE IS based on GENERATED code BY ObjectBox

/**
 * Cursor for DB entity "TestEntity".
 */
public final class TestEntityCursor extends Cursor<TestEntity> {
    public static boolean INT_NULL_HACK;

    @Internal
    static final class Factory implements CursorFactory<TestEntity> {
        public Cursor<TestEntity> createCursor(Transaction tx, long cursorHandle, BoxStore boxStoreForEntities) {
            return new TestEntityCursor(tx, cursorHandle, boxStoreForEntities);
        }
    }

    private static final TestEntity_ PROPERTIES = new TestEntity_();

    private static final TestEntity_.TestEntityIdGetter ID_GETTER = PROPERTIES.__ID_GETTER;


    // Property IDs get verified in Cursor base class
    private final static int __ID_simpleBoolean = TestEntity_.simpleBoolean.id;
    private final static int __ID_simpleByte = TestEntity_.simpleByte.id;
    private final static int __ID_simpleShort = TestEntity_.simpleShort.id;
    private final static int __ID_simpleInt = TestEntity_.simpleInt.id;
    private final static int __ID_simpleLong = TestEntity_.simpleLong.id;
    private final static int __ID_simpleFloat = TestEntity_.simpleFloat.id;
    private final static int __ID_simpleDouble = TestEntity_.simpleDouble.id;
    private final static int __ID_simpleString = TestEntity_.simpleString.id;
    private final static int __ID_simpleByteArray = TestEntity_.simpleByteArray.id;

    public TestEntityCursor(Transaction tx, long cursor, BoxStore boxStore) {
        super(tx, cursor, PROPERTIES, boxStore);
    }

    @Override
    public final long getId(TestEntity entity) {
        return ID_GETTER.getId(entity);
    }

    /**
     * Puts an object into its box.
     *
     * @return The ID of the object within its box.
     */
    @Override
    public final long put(TestEntity entity) {
        long __assignedId = collect313311(cursor, entity.getId(), PUT_FLAG_FIRST | PUT_FLAG_COMPLETE,
                9, entity.getSimpleString(), 0, null, 0, null,
                10, entity.getSimpleByteArray(),
                0, 0, 6, entity.getSimpleLong(), INT_NULL_HACK ? 0 : 5, entity.getSimpleInt(),
                4, entity.getSimpleShort(), 3, entity.getSimpleByte(),
                2, entity.getSimpleBoolean() ? 1 : 0,
                7, entity.getSimpleFloat(), 8, entity.getSimpleDouble()
        );
        entity.setId(__assignedId);
        return __assignedId;
    }

    // TODO do we need this? @Override
    protected final boolean isEntityUpdateable() {
        return true;
    }

}
