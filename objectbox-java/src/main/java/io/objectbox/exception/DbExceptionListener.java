/*
 * Copyright 2018-2020 ObjectBox Ltd. All rights reserved.
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

import io.objectbox.annotation.apihint.Experimental;

/**
 * Listener for exceptions occurring during database operations.
 * Set via {@link io.objectbox.BoxStore#setDbExceptionListener(DbExceptionListener)}.
 */
public interface DbExceptionListener {
    /**
     * WARNING/DISCLAIMER: Please avoid this method and handle exceptions "properly" instead.
     * By using this method, you "hack" into the exception handling by preventing native core exceptions to be
     * raised in Java. This typically results in methods returning zero or null regardless if this breaks any
     * non-zero or non-null contract that would be in place otherwise. Additionally, "canceling" exceptions
     * may lead to unforeseen follow-up errors that would never occur otherwise. In short, by using this method
     * you are accepting undefined behavior.
     * <p>
     * Also note that it is likely that this method will never graduate from @{@link Experimental} until it is removed.
     * <p>
     * This method may be only called from {@link #onDbException(Exception)}.
     */
    @Experimental
    static void cancelCurrentException() {
        DbExceptionListenerJni.nativeCancelCurrentException();
    }

    /**
     * Called when an exception is thrown during a database operation.
     * Do NOT throw exceptions in this method: throw exceptions are ignored (but logged to stderr).
     *
     * @param e the exception occurred during a database operation
     */
    void onDbException(Exception e);
}

/**
 * Interface cannot have native methods.
 */
class DbExceptionListenerJni {
    native static void nativeCancelCurrentException();

    native static void nativeThrowException(long nativeStore, int exNo);
}
