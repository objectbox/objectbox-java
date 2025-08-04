/*
 * Copyright 2024 ObjectBox Ltd.
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
 * Thrown when a special feature was used, which is not part of the native library.
 * <p>
 * This typically indicates a developer error. Check that the correct dependencies for the native ObjectBox library are
 * included.
 */
public class FeatureNotAvailableException extends DbException {

    // Note: this constructor is called by JNI, check before modifying/removing it.
    public FeatureNotAvailableException(String message) {
        super(message);
    }

}
