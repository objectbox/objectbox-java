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

import io.objectbox.annotation.apihint.Beta;

/**
 * Defines a backlink relation, which is based on another relation reversing the direction.
 * <p>
 * Example (to-one relation): one "Order" references one "Customer".
 * The backlink to this is a to-many in the reverse direction: one "Customer" has a number of "Order"s.
 * <p>
 * Example (to-many relation): one "Teacher" references multiple "Student"s.
 * The backlink to this: one "Student" has a number of "Teacher"s.
 * <p>
 * Note: changes made to a backlink relation based on a to-many relation are ignored.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.FIELD)
@Beta
public @interface Backlink {
    /**
     * Name of the relation the backlink should be based on (e.g. name of a ToOne or ToMany property in the target entity).
     * Can be left empty if there is just a single relation from the target to the source entity.
     */
    String to() default "";
}
