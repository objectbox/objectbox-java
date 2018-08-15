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

package io.objectbox.exception;

import io.objectbox.BoxStore;
import io.objectbox.BoxStoreBuilder;

/**
 * Thrown when the maximum of readers (read transactions) was exceeded.
 * Verify that you run a reasonable amount of threads only.
 * <p>
 * If you intend to work with a very high number of threads (&gt;100), consider increasing the number of maximum readers
 * using {@link BoxStoreBuilder#maxReaders(int)} and enabling query retries using
 * {@link BoxStoreBuilder#queryAttempts(int)}.
 * <p>
 * For debugging issues related to this exception, check {@link BoxStore#diagnose()}.
 */
public class DbMaxReadersExceededException extends DbException {
    public DbMaxReadersExceededException(String message) {
        super(message);
    }

    public DbMaxReadersExceededException(String message, int errorCode) {
        super(message, errorCode);
    }
}
