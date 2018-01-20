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

package io.objectbox.exception;

/**
 * Listener for exceptions occurring during database operations.
 * Set via {@link io.objectbox.BoxStore#setDbExceptionListener(DbExceptionListener)}.
 */
public interface DbExceptionListener {
    /**
     * Called when an exception is thrown during a database operation.
     * Do NOT throw exceptions in this method: throw exceptions are ignored (but logged to stderr).
     *
     * @param e the exception occurred during a database operation
     */
    void onDbException(Exception e);
}
