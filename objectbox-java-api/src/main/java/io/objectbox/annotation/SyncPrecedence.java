/*
 * Copyright © 2026 ObjectBox Ltd. <https://objectbox.io>
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
 * Marks a {@code long} (64-bit integer) field of a {@link Sync}-enabled {@link Entity} class as the "sync precedence"
 * to customize Sync conflict resolution.
 * <p>
 * Developer-assigned precedence values are then used to resolve conflicts via "higher precedence wins". Defining and
 * assigning precedence values are completely in the hands of the developer (the ObjectBox user).
 * <p>
 * There can be only one sync precedence per sync entity type.
 * <p>
 * Typically, it is combined with a {@link SyncClock}, with the latter being the tie-breaker for equal precedence
 * values.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.FIELD)
public @interface SyncPrecedence {
}
