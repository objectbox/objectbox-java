/*
 * Copyright 2018-2025 ObjectBox Ltd.
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

import org.junit.Before;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import io.objectbox.AbstractObjectBoxTest;
import io.objectbox.Box;
import io.objectbox.BoxStoreBuilder;
import io.objectbox.TestEntity;
import io.objectbox.annotation.IndexType;
import io.objectbox.config.DebugFlags;

public class AbstractQueryTest extends AbstractObjectBoxTest {
    protected Box<TestEntity> box;

    @Override
    protected BoxStoreBuilder createBoxStoreBuilder(@Nullable IndexType simpleStringIndexType) {
        BoxStoreBuilder builder = super.createBoxStoreBuilder(simpleStringIndexType);
        if (DEBUG_LOG) builder.debugFlags(DebugFlags.LOG_QUERY_PARAMETERS);
        return builder;
    }

    @Before
    public void setUpBox() {
        box = getTestEntityBox();
    }

    /**
     * Puts 10 TestEntity starting at nr 2000 using {@link AbstractObjectBoxTest#createTestEntity(String, int)}.
     * <li>simpleInt = [2000..2009]</li>
     * <li>simpleByte = [2010..2019]</li>
     * <li>simpleBoolean = [true, false, ..., false]</li>
     * <li>simpleShort = [2100..2109]</li>
     * <li>simpleLong = [3000..3009]</li>
     * <li>simpleFloat = [400.0..400.9]</li>
     * <li>simpleDouble = [2020.00..2020.09] (approximately)</li>
     * <li>simpleByteArray = [{1,2,2000}..{1,2,2009}]</li>
     * <li>boolArray = [{true, false, true}..{false, false, true}]</li>
     * <li>shortArray = [{-2100,2100}..{-2109,2109}]</li>
     * <li>intArray = [{-2000,2000}..{-2009,2009}]</li>
     * <li>longArray = [{-3000,3000}..{-3009,3009}]</li>
     * <li>floatArray = [{-400.0,400.0}..{-400.9,400.9}]</li>
     * <li>doubleArray = [{-2020.00,2020.00}..{-2020.09,2020.09}] (approximately)</li>
     * <li>date = [Date(3000)..Date(3009)]</li>
     */
    public List<TestEntity> putTestEntitiesScalars() {
        return putTestEntities(10, null, 2000);
    }

    /**
     * Puts 5 TestEntity starting at nr 1 using {@link AbstractObjectBoxTest#createTestEntity(String, int)}.
     * <li>simpleString = banana, apple, bar, banana milk shake, foo bar</li>
     * <li>simpleStringArray = [simpleString]</li>
     * <li>simpleStringList = [simpleString]</li>
     * <li>charArray = simpleString.toCharArray()</li>
     */
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
