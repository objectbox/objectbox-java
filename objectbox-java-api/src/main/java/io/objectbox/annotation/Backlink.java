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
 * Example: one "Order" references one "Customer" (to-one relation).
 * The backlink to this is a to-many in the reverse direction: one "Customer" has a number of "Order"s.
 *
 * Note: backlinks to to-many relations will be supported in the future.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.FIELD)
@Beta
public @interface Backlink {
    /**
     * Name of the relation the backlink should be based on (e.g. name of a ToOne property in the target entity).
     * Can be left empty if there is just a single relation from the target to the source entity.
     */
    String to() default "";
}
