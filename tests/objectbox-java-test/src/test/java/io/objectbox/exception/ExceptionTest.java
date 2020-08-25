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

import org.junit.Test;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import io.objectbox.AbstractObjectBoxTest;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Tests related to {@link DbExceptionListener}.
 */
public class ExceptionTest extends AbstractObjectBoxTest {

    @Test
    public void exceptionListener_null_works() {
        store.setDbExceptionListener(null);
    }

    @Test
    public void exceptionListener_closedStore_works() {
        store.close();
        store.setDbExceptionListener(e -> System.out.println("This is never called"));
    }

    private static boolean weakRefListenerCalled = false;

    @Test
    public void exceptionListener_noLocalRef_works() throws InterruptedException {
        // Note: do not use lambda, it would keep a reference to this class
        // and prevent garbage collection of the listener.
        //noinspection Convert2Lambda
        DbExceptionListener listenerNoRef = new DbExceptionListener() {
            @Override
            public void onDbException(Exception e) {
                System.out.println("Listener without strong reference is called");
                weakRefListenerCalled = true;
            }
        };
        WeakReference<DbExceptionListener> weakReference = new WeakReference<>(listenerNoRef);

        // Set and clear local reference.
        store.setDbExceptionListener(listenerNoRef);
        //noinspection UnusedAssignment
        listenerNoRef = null;

        // Try and fail to release weak reference.
        int triesClearWeakRef = 5;
        while (weakReference.get() != null) {
            if (--triesClearWeakRef == 0) break;
            System.out.println("Suggesting GC");
            System.gc();
            //noinspection BusyWait
            Thread.sleep(300);
        }
        assertEquals("Failed to keep weak reference to listener", 0, triesClearWeakRef);

        // Throw, listener should be called.
        weakRefListenerCalled = false;
        assertThrows(
                DbException.class,
                () -> DbExceptionListenerJni.nativeThrowException(store.getNativeStore(), 0)
        );
        assertTrue(weakRefListenerCalled);

        // Remove reference from native side.
        store.setDbExceptionListener(null);

        // Try and succeed to release weak reference.
        triesClearWeakRef = 5;
        while (weakReference.get() != null) {
            if (--triesClearWeakRef == 0) break;
            System.out.println("Suggesting GC");
            System.gc();
            //noinspection BusyWait
            Thread.sleep(300);
        }
        assertTrue("Failed to release weak reference to listener", triesClearWeakRef > 0);

        // Throw, listener should not be called.
        weakRefListenerCalled = false;
        assertThrows(
                DbException.class,
                () -> DbExceptionListenerJni.nativeThrowException(store.getNativeStore(), 0)
        );
        assertFalse(weakRefListenerCalled);
    }

    @Test
    public void exceptionListener_removing_works() {
        AtomicBoolean replacedListenerCalled = new AtomicBoolean(false);
        DbExceptionListener listenerRemoved = e -> replacedListenerCalled.set(true);

        store.setDbExceptionListener(listenerRemoved);
        store.setDbExceptionListener(null);

        assertThrows(
                DbException.class,
                () -> DbExceptionListenerJni.nativeThrowException(store.getNativeStore(), 0)
        );
        assertFalse("Should not have called removed DbExceptionListener.", replacedListenerCalled.get());
    }

    @Test
    public void exceptionListener_replacing_works() {
        AtomicBoolean replacedListenerCalled = new AtomicBoolean(false);
        DbExceptionListener listenerReplaced = e -> replacedListenerCalled.set(true);

        AtomicBoolean newListenerCalled = new AtomicBoolean(false);
        DbExceptionListener listenerNew = e -> newListenerCalled.set(true);

        store.setDbExceptionListener(listenerReplaced);
        store.setDbExceptionListener(listenerNew);

        assertThrows(
                DbException.class,
                () -> DbExceptionListenerJni.nativeThrowException(store.getNativeStore(), 0)
        );
        assertFalse("Should not have called replaced DbExceptionListener.", replacedListenerCalled.get());
        assertTrue("Failed to call new DbExceptionListener.", newListenerCalled.get());
    }

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