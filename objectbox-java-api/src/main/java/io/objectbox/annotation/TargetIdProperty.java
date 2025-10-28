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
 * For a ToOne, changes the name of its associated target ID (or "relation") property.
 * <p>
 * <pre>{@code
 * @Entity
 * public class Order {
 *     // Change from default "customerId" to "customerRef"
 *     @TargetIdProperty("customerRef")
 *     ToOne<Customer> customer;
 *     // Optional: expose target ID property (using changed name)
 *     long customerRef;
 * }
 * }</pre>
 * <p>
 * A target ID property is implicitly created (so without defining it in the {@link Entity @Entity} class) for each
 * ToOne and stores the ID of the referenced target object. By default, it's named like the ToOne field plus the suffix
 * "Id" (for example {@code customerId}).
 * <p>
 * Like in the example above, it's still possible to expose the target ID property as an actual field (useful for other
 * parsers or serializers, like for JSON). But make sure to use the changed name for the field.
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
