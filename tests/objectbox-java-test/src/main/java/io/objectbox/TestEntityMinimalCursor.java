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
import io.objectbox.internal.IdGetter;

public class TestEntityMinimalCursor extends Cursor<TestEntityMinimal> {
    private static final TestEntityMinimal_ PROPERTIES = new TestEntityMinimal_();

    @Internal
    static final class Factory implements CursorFactory<TestEntityMinimal> {
        public Cursor<TestEntityMinimal> createCursor(Transaction tx, long cursorHandle, BoxStore boxStoreForEntities) {
            return new TestEntityMinimalCursor(tx, cursorHandle, boxStoreForEntities);
        }
    }

    public TestEntityMinimalCursor(Transaction tx, long cursor, BoxStore boxStore) {
        super(tx, cursor, PROPERTIES, boxStore);
    }

    @Override
    protected long getId(TestEntityMinimal entity) {
        return entity.getId();
    }

    public long put(TestEntityMinimal entity) {
        long key = entity.getId();
        key = collect313311(cursor, key, PUT_FLAG_FIRST | PUT_FLAG_COMPLETE,
                2, entity.getText(), 0, null, 0, null,
                0, null, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        );
        entity.setId(key);
        return key;
    }

}
