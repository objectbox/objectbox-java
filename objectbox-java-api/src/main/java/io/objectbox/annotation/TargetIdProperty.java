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
 * For a ToOne, changes the name of the property to store the target object ID in from the default name:
 * <p>
 * <pre>{@code
 * @Entity
 * public class Order {
 *     @TargetIdProperty("customerRenamedTargetId")
 *     ToOne<Customer> customer;
 *     long customerRenamedTargetId;
 * }
 * }</pre>
 * <p>
 * By default, a target ID property named like the ToOne property with the suffix "Id" is created implicitly (so without
 * defining it in the @Entity class). Using the example above, without the annotation a "virtual" property named
 * {@code long customerId} would be created.
 * <p>
 * Exposing the target ID as an actual field (so not necessarily renamed using this annotation) can be useful for other
 * parsers or serializers, like for JSON.
 * <p>
 * See the <a href="https://docs.objectbox.io/relations">relations documentation</a> for details.
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface TargetIdProperty {
    /**
     * Name used in the database.
     */
    String value();

}
