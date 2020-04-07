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
 * Specifies ordering of related collection of {@link Relation} relation
 * E.g.: @OrderBy("name, age DESC") List collection;
 * If used as marker (@OrderBy List collection), then collection is ordered by primary key
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.FIELD)
/* TODO public */ @interface OrderBy {
    /**
     * Comma-separated list of properties, e.g. "propertyA, propertyB, propertyC"
     * To specify direction, add ASC or DESC after property name, e.g.: "propertyA DESC, propertyB ASC"
     * Default direction for each property is ASC
     * If value is omitted, then collection is ordered by primary key
     */
    String value() default "";
}
