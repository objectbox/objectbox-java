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
 * Marks a {@code long} (64-bit integer) field of a {@link Sync}-enabled {@link Entity} class as the sync clock, a
 * "hybrid logical clock" to resolve Sync conflicts.
 * <p>
 * These clock values allow "last write wins" conflict resolution.
 * <p>
 * There can be only one sync clock per sync entity type; which is also recommended for basic conflict resolution.
 * <p>
 * For new objects, initialize the property value to {@code 0} to reserve "a slot" in the object data. ObjectBox Sync
 * will update this property automatically on put operations.
 * <p>
 * As a hybrid clock, it combines a wall clock with a logical counter to compensate for some clock skew effects.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.FIELD)
public @interface SyncClock {
}
