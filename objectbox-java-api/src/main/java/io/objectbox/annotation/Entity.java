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
 * Marks a class as an ObjectBox Entity.
 * Allows to obtain a Box for this Entity from BoxStore to persist Objects of this class.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface Entity {

// Maybe better than "createInDb":
// Class partialFor" to create a partial class referencing the class to the full entity
//    /**
//     * Advanced flag to disable table creation in the database (when set to false). This can be used to create partial
//     * entities, which may use only a sub set of properties. Be aware however that ObjectBox does not sync multiple
//     * entities, e.g. in caches.
//     */
//    boolean createInDb() default true;
//
//    /**
//     * Specifies schema name for the entity: ObjectBox can generate independent sets of classes for each schema.
//     * Entities which belong to different schemas should <strong>not</strong> have relations.
//     */
//    String schema() default "default";

    /**
     * Use a no-arg constructor instead of an all properties constructor (generated).
     * Note that is generally not recommended when using the ObjectBox Gradle plugin for Java classes.
     */
    boolean useNoArgConstructor() default false;

//
//    /**
//     * Define a protobuf class of this entity to create an additional, special DAO for.
//     */
//    Class protobuf() default void.class;

}
