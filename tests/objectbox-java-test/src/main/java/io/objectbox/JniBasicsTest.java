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

import org.junit.Before;
import org.junit.Test;

import io.objectbox.internal.JniTest;


import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class JniBasicsTest {
    @Before
    public void loadLib() {
        BoxStore.getVersionNative();
    }

    @Test
    public void testReturnIntArray() {
        // Lower Android versions have a ReferenceTable with 1024 entries only
        for (int i = 0; i < 2000; i++) {
            int[] ints = JniTest.returnIntArray();
            assertNotNull(ints);
        }
    }

    @Test
    public void testCreateAndDeleteIntArray() {
        // Lower Android versions have a ReferenceTable with 1024 entries only
        for (int i = 0; i < 2000; i++) {
            assertTrue(JniTest.createAndDeleteIntArray());
            System.out.print(i);
        }
    }
}
