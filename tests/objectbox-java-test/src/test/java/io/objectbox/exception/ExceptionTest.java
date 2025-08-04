/*
 * Copyright 2020 ObjectBox Ltd.
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
import java.util.concurrent.atomic.AtomicInteger;

import io.objectbox.AbstractObjectBoxTest;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Tests related to {@link DbExceptionListener}.
 *
 * Note: this test has an equivalent in test-java-android integration tests.
 */
public class ExceptionTest extends AbstractObjectBoxTest {

    @Test
    public void exceptionListener_null_works() {
        store.setDbExceptionListener(null);
    }

    @Test(expected = IllegalStateException.class)
    public void exceptionListener_closedStore_throws() {
        store.close();
        store.setDbExceptionListener(e -> System.out.println("This is never called"));
    }

    private static final AtomicInteger weakRefListenerCalled = new AtomicInteger(0);

    @Test
    public void exceptionListener_noLocalRef_works() throws InterruptedException {
        weakRefListenerCalled.set(0);
        // Note: do not use lambda, it would keep a reference to this class
        // and prevent garbage collection of the listener.
        //noinspection Convert2Lambda
        DbExceptionListener listenerNoRef = new DbExceptionListener() {
            @Override
            public void onDbException(Exception e) {
                System.out.println("Listener without strong reference is called");
                weakRefListenerCalled.incrementAndGet();
            }
        };
        WeakReference<DbExceptionListener> weakReference = new WeakReference<>(listenerNoRef);

        // Set and clear local reference.
        store.setDbExceptionListener(listenerNoRef);
        //noinspection UnusedAssignment
        listenerNoRef = null;

        // Ensure weak reference is kept as JNI is holding on to listener using a "global ref".
        int triesClearWeakRef = 5;
        while (weakReference.get() != null) {
            if (--triesClearWeakRef == 0) break;
            System.out.println("Suggesting GC");
            System.gc();
            System.runFinalization();
            //noinspection BusyWait
            Thread.sleep(300);
        }
        assertEquals("Failed to keep weak reference to listener", 0, triesClearWeakRef);
        assertNotNull(weakReference.get());

        // Throw, listener should be called.
        assertThrows(
                DbException.class,
                () -> DbExceptionListenerJni.nativeThrowException(store.getNativeStore(), 0)
        );
        assertEquals(1, weakRefListenerCalled.get());

        // Remove reference from native side.
        store.setDbExceptionListener(null);

        // Try and succeed to release weak reference.
        triesClearWeakRef = 5;
        while (weakReference.get() != null) {
            if (--triesClearWeakRef == 0) break;
            System.out.println("Suggesting GC");
            System.gc();
            System.runFinalization();
            //noinspection BusyWait
            Thread.sleep(300);
        }
        assertTrue("Failed to release weak reference to listener", triesClearWeakRef > 0);

        // Throw, listener should not be called.
        assertThrows(
                DbException.class,
                () -> DbExceptionListenerJni.nativeThrowException(store.getNativeStore(), 0)
        );
        assertEquals(1, weakRefListenerCalled.get());
    }

    @Test
    public void exceptionListener_noref() throws InterruptedException {
        weakRefListenerCalled.set(0);

        //noinspection Convert2Lambda
        store.setDbExceptionListener(new DbExceptionListener() {
            @Override
            public void onDbException(Exception e) {
                System.out.println("Listener without reference is called");
                weakRefListenerCalled.incrementAndGet();
            }
        });

        for (int i = 0; i < 5; i++) {
            System.gc();
            System.runFinalization();
            Thread.sleep(100);
        }

        // Throw, listener should be called.
        assertThrows(
                DbException.class,
                () -> DbExceptionListenerJni.nativeThrowException(store.getNativeStore(), 0)
        );
        assertEquals(1, weakRefListenerCalled.get());
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