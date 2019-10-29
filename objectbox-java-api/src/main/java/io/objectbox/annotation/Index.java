/*
 * Copyright 2017-2018 ObjectBox Ltd. All rights reserved.
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
 * Specifies that the property should be indexed.
 * <p>
 * It is highly recommended to index properties that are used in a query to improve query performance.
 * <p>
 * To fine tune indexing of a property you can override the default index {@link #type()}.
 * <p>
 * Note: indexes are currently not supported for byte array, float or double properties.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.FIELD)
public @interface Index {
    /**
     * Sets the {@link IndexType}, defaults to {@link IndexType#DEFAULT}.
     */
    IndexType type() default IndexType.DEFAULT;
}
