/*
 * Copyright 2019 ObjectBox Ltd.
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
 * Use on a property to override how its value is stored and interpreted in the database.
 * <p>
 * For example to change a long to be interpreted as nanoseconds instead of milliseconds:
 * <pre>
 * &#064;Type(DatabaseType.DateNano)
 * public long timeInNanos;
 * </pre>
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.FIELD})
public @interface Type {

    DatabaseType value();

}
