/*
 * Copyright 2019 ObjectBox Ltd.
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

package io.objectbox.annotation;

/**
 * Use with {@link Type @Type} to override how a property value is stored and interpreted in the database.
 */
public enum DatabaseType {

    /**
     * Use with 64-bit long properties to store them as high precision time
     * representing nanoseconds since 1970-01-01 (unix epoch).
     * <p>
     * By default, a 64-bit long value is interpreted as time in milliseconds (a Date).
     */
    DateNano

}
