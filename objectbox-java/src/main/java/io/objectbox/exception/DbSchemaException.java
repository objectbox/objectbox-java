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
 * Thrown when there is an error with the data schema (data model).
 * <p>
 * Typically, there is a conflict between the data model defined in your code (using {@link io.objectbox.annotation.Entity @Entity}
 * classes) and the data model of the existing database file.
 * <p>
 * Read the <a href="https://docs.objectbox.io/advanced/meta-model-ids-and-uids#resolving-meta-model-conflicts">meta model docs</a>
 * on why this can happen and how to resolve such conflicts.
 */
public class DbSchemaException extends DbException {
    public DbSchemaException(String message) {
        super(message);
    }
}
