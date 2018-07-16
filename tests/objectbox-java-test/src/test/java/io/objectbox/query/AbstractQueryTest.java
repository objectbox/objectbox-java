/*
 * Copyright 2018 ObjectBox Ltd. All rights reserved.
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

package io.objectbox.query;

import io.objectbox.*;
import org.junit.Before;

import java.util.ArrayList;
import java.util.List;

public class AbstractQueryTest extends AbstractObjectBoxTest {
    protected Box<TestEntity> box;

    @Override
    protected BoxStoreBuilder createBoxStoreBuilder(boolean withIndex) {
        return super.createBoxStoreBuilder(withIndex).debugFlags(DebugFlags.LOG_QUERY_PARAMETERS);
    }

    @Before
    public void setUpBox() {
        box = getTestEntityBox();
    }

    /**
     * Puts 10 TestEntity starting at nr 2000 using {@link AbstractObjectBoxTest#createTestEntity(String, int)}.
     */
    List<TestEntity> putTestEntitiesScalars() {
        return putTestEntities(10, null, 2000);
    }

    List<TestEntity> putTestEntitiesStrings() {
        List<TestEntity> entities = new ArrayList<>();
        entities.add(createTestEntity("banana", 1));
        entities.add(createTestEntity("apple", 2));
        entities.add(createTestEntity("bar", 3));
        entities.add(createTestEntity("banana milk shake", 4));
        entities.add(createTestEntity("foo bar", 5));
        box.put(entities);
        return entities;
    }
}
