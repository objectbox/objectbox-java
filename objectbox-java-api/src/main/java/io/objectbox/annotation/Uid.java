/*
 * Copyright 2017 ObjectBox Ltd.
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
 * UIDs identify entities (and properties and relations) uniquely in the meta object model file
 * (objectbox-model/default.json). With UIDs entities can be mapped to their meta model
 * representation in a stable way without their name. Once a UID is set, the entity can be renamed
 * as often as you like - ObjectBox keeps track of it automatically. Thus, it is advisable to look
 * up the UID in the model file and use it here before renaming an entity.
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface Uid {
    /**
     * The UID associated with an entity, property or relation, as found in the model file.
     * <p>
     * Special values:
     * <ul>
     * <li>no value, 0 or -1: the build fails with a message that shows the UID operations for this
     * entity, property or relation.</li>
     * </ul>
     * <p>
     * See the <a href="https://docs.objectbox.io/advanced/data-model-updates">Data Model Updates</a>
     * documentation for details.
     */
    long value() default 0;
}
