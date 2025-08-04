/*
 * Copyright 2025 ObjectBox Ltd.
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
 * Sets the type of a property or the type of object IDs of a ToMany in an external system (like another database).
 * <p>
 * This is useful if there is no default mapping of the ObjectBox type to the type in the external system.
 * <p>
 * Carefully look at the documentation of the external type to ensure it is compatible with the ObjectBox type.
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.FIELD})
public @interface ExternalType {

    ExternalPropertyType value();

}
