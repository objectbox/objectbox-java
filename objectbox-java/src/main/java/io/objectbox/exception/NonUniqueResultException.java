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
 * Thrown if {@link io.objectbox.query.Query#findUnique() Query.findUnique()} or
 * {@link io.objectbox.query.Query#findUniqueId() Query.findUniqueId()} is called,
 * but the query matches more than one object.
 */
public class NonUniqueResultException extends DbException {
    public NonUniqueResultException(String message) {
        super(message);
    }
}
