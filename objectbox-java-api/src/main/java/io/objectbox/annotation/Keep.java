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
 * Specifies that the target should be kept during next run of ObjectBox generation.
 * <p>
 * Using this annotation on an Entity class itself silently disables any class modification.
 * The user is responsible to write and support any code which is required for ObjectBox.
 * </p>
 * <p>
 * Don't use this annotation on a class member if you are not completely sure what you are doing, because in
 * case of model changes ObjectBox will not be able to make corresponding changes into the code of the target.
 * </p>
 *
 * @see Generated
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.FIELD, ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.TYPE})
@Deprecated
public @interface Keep {
}
