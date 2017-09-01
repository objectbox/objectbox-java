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

package io.objectbox.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks field is the primary key of the entity's table
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.FIELD)
public @interface Id {
//    /**
//     * Specifies that id should increase monotonic without reusing IDs. This decreases performance a little bit for
//     * putting new objects (inserts) because the state needs to be persisted. Gaps between two IDs may still occur,
//     * e.g. if inserts are rollbacked.
//     */
//    boolean monotonic() default false;

    /**
     * Allows IDs to be assigned by the developer. This may make sense for using IDs originating somewhere else, e.g.
     * from the server.
     */
    boolean assignable() default false;
}
