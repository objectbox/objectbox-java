/*
 * Copyright 2017-2025 ObjectBox Ltd.
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

package io.objectbox.exception;

/**
 * This exception occurs while working with a {@link io.objectbox.relation.ToMany ToMany} or
 * {@link io.objectbox.relation.ToOne ToOne} of an object and the object is not attached to a
 * {@link io.objectbox.Box Box} (technically a {@link io.objectbox.BoxStore BoxStore}).
 * <p>
 * If your code uses <a href="https://docs.objectbox.io/advanced/object-ids#self-assigned-object-ids">manually assigned
 * IDs</a> make sure it takes care of some things that ObjectBox would normally do by itself. This includes
 * {@link io.objectbox.Box#attach(Object) attaching} the Box to an object before modifying a ToMany.
 * <p>
 * Also see the documentation about <a href="https://docs.objectbox.io/relations#updating-relations">Updating
 * Relations</a> and <a href="https://docs.objectbox.io/advanced/object-ids#self-assigned-object-ids">manually assigned
 * IDs</a> for details.
 */
public class DbDetachedException extends DbException {

    public DbDetachedException() {
        this("Entity must be attached to a Box.");
    }

    public DbDetachedException(String message) {
        super(message);
    }

}
