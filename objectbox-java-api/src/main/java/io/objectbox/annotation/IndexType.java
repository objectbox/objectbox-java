/*
 * Copyright 2018 ObjectBox Ltd. All rights reserved.
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

/**
 * ObjectBox offers a value and two hash index types, from which it chooses a reasonable default (see {@link #DEFAULT}).
 * <p>
 * For some queries/use cases it might make sense to override the default choice for optimization purposes.
 * <p>
 * Note: hash indexes are currently only supported for string properties.
 */
public enum IndexType {
    /**
     * Use the default index type depending on the property type:
     * {@link #VALUE} for scalars and {@link #HASH} for Strings.
     */
    DEFAULT,

    /**
     * Use the property value to build the index.
     * For Strings this may occupy more space than the default setting.
     */
    VALUE,

    /**
     * Use a (fast non-cryptographic) hash of the property value to build the index.
     * Internally, it uses a 32 bit hash with a decent hash collision behavior.
     * Because occasional collisions do not really impact performance, this is usually a better choice than
     * {@link #HASH64} as it takes less space.
     */
    HASH,

    /**
     * Use a long (fast non-cryptographic) hash of the property value to build the index.
     */
    HASH64
}
