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
 * UIDs identify entities (and properties) uniquely in the meta object model file (objectbox-model/default.json).
 * With UIDs you can map entities to their meta model representation in a stable way without its name.
 * Once a UID is set, you can rename the entity as often as you like - ObjectBox keeps track of it automatically.
 * Thus, it is advisable to lookup the UID in objectbox-model/default.json and use it here before renaming a entity.
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface Uid {
    /**
     * The UID associated with an entity/property.
     * <p>
     * Special values:
     * <ul>
     * <li>empty (or zero): and the ObjectBox Gradle plugin will set it automatically to the current value.</li>
     * <li>-1: will assign a new ID and UID forcing the property/entity to be treated as new
     * (for entities: all property IDs and UIDs will be renewed too)</li>
     * </ul>
     */
    long value() default 0;
}
