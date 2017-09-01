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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines name and referencedName properties for relations
 *
 * @see Relation
 */
@Retention(RetentionPolicy.CLASS)
@Target({})
/** TODO public */ @interface JoinProperty {
    /** Name of the property in the name entity, which matches {@link #referencedName()} */
    String name();

    /** Name of the property in the referencedName entity, which matches {@link #name()} */
    String referencedName();
}
