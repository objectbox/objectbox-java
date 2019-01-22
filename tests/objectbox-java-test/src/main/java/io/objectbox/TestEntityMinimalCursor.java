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

public class TestEntityMinimalCursor extends Cursor<TestEntityMinimal> {

    @Internal
    static final class Factory implements CursorFactory<TestEntityMinimal> {
        public Cursor<TestEntityMinimal> createCursor(Transaction tx, long cursorHandle, BoxStore boxStoreForEntities) {
            return new TestEntityMinimalCursor(tx, cursorHandle, boxStoreForEntities);
        }
    }

    private final static int __ID_test = TestEntityMinimal_.text.id;

    public TestEntityMinimalCursor(Transaction tx, long cursor, BoxStore boxStore) {
        super(tx, cursor, TestEntityMinimal_.__INSTANCE, boxStore);
    }

    @Override
    protected long getId(TestEntityMinimal entity) {
        return entity.getId();
    }

    /**
     * Puts an object into its box.
     *
     * @return The ID of the object within its box.
     */
    @Override
    public long put(TestEntityMinimal entity) {
        long __assignedId = collect313311(cursor, entity.getId(), PUT_FLAG_FIRST | PUT_FLAG_COMPLETE,
                __ID_test, entity.getText(), 0, null,
                0, null, 0, null,
                0, 0, 0, 0,
                0, 0, 0, 0,
                0, 0, 0, 0,
                0, 0, 0, 0);
        entity.setId(__assignedId);
        return __assignedId;
    }

}
