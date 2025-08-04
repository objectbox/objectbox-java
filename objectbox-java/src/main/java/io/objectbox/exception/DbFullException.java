/*
 * Copyright 2017-2024 ObjectBox Ltd.
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
 * Thrown when applying a database operation would exceed the (default)
 * {@link io.objectbox.BoxStoreBuilder#maxSizeInKByte(long) maxSizeInKByte} configured for the Store.
 * <p>
 * This can occur for operations like when an Object is {@link io.objectbox.Box#put(Object) put}, at the point when the
 * (internal) transaction is committed. Or when the Store is opened with a max size too small for the existing database.
 */
public class DbFullException extends DbException {
    public DbFullException(String message) {
        super(message);
    }

    public DbFullException(String message, int errorCode) {
        super(message, errorCode);
    }

}
