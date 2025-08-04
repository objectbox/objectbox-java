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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that values of an integer property (e.g. int and long in Java)
 * should be treated as unsigned when doing queries or creating indexes.
 * <p>
 * For example consider a set of integers sorted by the default order: [-2, -1, 0, 1, 2].
 * If all values are treated as unsigned, the order would be [0, 1, 2, -2, -1] instead.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.FIELD)
public @interface Unsigned {
}
