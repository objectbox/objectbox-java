/*
 * Copyright 2020 ObjectBox Ltd.
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
 * Enables sync for an {@link Entity} class.
 * <p>
 * Note that currently sync can not be enabled or disabled for existing entities.
 * Also synced entities can not have relations to non-synced entities.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface Sync {

    /**
     * Set to {@code true} to enable shared global IDs for a {@link Sync}-enabled {@link Entity} class.
     * <p>
     * By default each Sync client has its own local {@link Id ID} space for Objects.
     * IDs are mapped to global IDs when syncing behind the scenes. Turn this on
     * to treat Object IDs as global and turn of ID mapping. The ID of an Object will
     * then be the same on all clients.
     * <p>
     * When using this, it is recommended to use {@link Id#assignable() assignable IDs}
     * to turn off automatically assigned IDs. Without special care, two Sync clients are
     * likely to overwrite each others Objects if IDs are assigned automatically.
     */
    boolean sharedGlobalIds() default false;
}
