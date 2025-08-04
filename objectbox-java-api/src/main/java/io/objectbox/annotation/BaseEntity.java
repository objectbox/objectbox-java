/*
 * Copyright 2017-2020 ObjectBox Ltd.
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
 * Marks a class as an ObjectBox Entity super class.
 * ObjectBox will store properties of this class as part of an Entity that inherits from this class.
 * See <a href="https://docs.objectbox.io/advanced/entity-inheritance">the Entity Inheritance documentation</a> for details.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface BaseEntity {
}
