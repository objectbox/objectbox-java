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

import io.objectbox.converter.PropertyConverter;

/**
 * Supplies a {@link PropertyConverter converter} to store custom Property types as a supported {@link #dbType()}.
 * See the <a href="https://docs.objectbox.io/advanced/custom-types">Custom Types documentation</a> for details.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.FIELD)
public @interface Convert {
    /**
     * The converter class that ObjectBox should use. See {@link PropertyConverter} for implementation guidelines.
     */
    Class<? extends PropertyConverter> converter();

    /**
     * The Property type the Java field value is converted to/from.
     * See the <a href="https://docs.objectbox.io/advanced/custom-types">Custom Types documentation</a> for a list
     * of supported types.
     */
    Class dbType();
}
