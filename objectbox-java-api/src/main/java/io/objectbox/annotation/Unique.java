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
 * Enforces that the value of a property is unique among all objects in a box before an object can be put.
 * <p>
 * Trying to put an object with offending values will result in a UniqueViolationException.
 * <p>
 * Unique properties are based on an {@link Index @Index}, so the same restrictions apply.
 * It is supported to explicitly add the {@link Index @Index} annotation to configure the index.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.FIELD)
public @interface Unique {
}
