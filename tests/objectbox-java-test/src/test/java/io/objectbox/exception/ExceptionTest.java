/*
 * Copyright 2020 ObjectBox Ltd. All rights reserved.
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

package io.objectbox.exception;

import io.objectbox.AbstractObjectBoxTest;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ExceptionTest extends AbstractObjectBoxTest {

    @Test
    public void testThrowExceptions() {
        final List<Exception> exs = new ArrayList<>();
        DbExceptionListener exceptionListener = e -> {
            exs.add(e);
            DbExceptionListener.cancelCurrentException();
        };
        store.setDbExceptionListener(exceptionListener);
        int maxExNo = 10;
        for (int exNo = 0; exNo <= maxExNo; exNo++) {
            DbExceptionListenerJni.nativeThrowException(store.getNativeStore(), exNo);
        }
        int expectedSize = maxExNo + 1;
        assertEquals(expectedSize, exs.size());
        Class<?>[] expectedClasses = {
                DbException.class,
                IllegalStateException.class,
                DbException.class,  // OpenDb
                DbFullException.class,
                DbShutdownException.class,
                DbSchemaException.class,
                ConstraintViolationException.class,
                UniqueViolationException.class,
                FileCorruptException.class,
                PagesCorruptException.class,
                IllegalArgumentException.class,
        };
        assertEquals(expectedSize, expectedClasses.length);
        for (int i = 0; i < expectedSize; i++) {
            assertEquals(expectedClasses[i], exs.get(i).getClass());
        }
    }

}