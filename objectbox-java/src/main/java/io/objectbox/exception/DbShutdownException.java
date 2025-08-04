/*
 * Copyright 2017 ObjectBox Ltd.
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
 * Thrown when an error occurred that requires the store to be closed.
 * <p>
 * This may be an I/O error. Regular operations won't be possible.
 * To handle this exit the app or try to reopen the store.
 */
public class DbShutdownException extends DbException {
    public DbShutdownException(String message) {
        super(message);
    }

    public DbShutdownException(String message, int errorCode) {
        super(message, errorCode);
    }

}
