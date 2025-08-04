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

import io.objectbox.BoxStoreBuilder;

/**
 * Errors were detected in a database file, e.g. illegal values or structural inconsistencies.
 * <p>
 * It may be possible to re-open the store with {@link BoxStoreBuilder#usePreviousCommit()} to restore
 * to a working state.
 */
public class FileCorruptException extends DbException {
    public FileCorruptException(String message) {
        super(message);
    }

    public FileCorruptException(String message, int errorCode) {
        super(message, errorCode);
    }
}
