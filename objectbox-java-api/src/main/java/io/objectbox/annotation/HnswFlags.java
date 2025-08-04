/*
 * Copyright 2024 ObjectBox Ltd.
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
 * Flags as a part of the {@link HnswIndex} configuration.
 */
public @interface HnswFlags {

    /**
     * Enables debug logs.
     */
    boolean debugLogs() default false;

    /**
     * Enables "high volume" debug logs, e.g. individual gets/puts.
     */
    boolean debugLogsDetailed() default false;

    /**
     * Padding for SIMD is enabled by default, which uses more memory but may be faster. This flag turns it off.
     */
    boolean vectorCacheSimdPaddingOff() default false;

    /**
     * If the speed of removing nodes becomes a concern in your use case, you can speed it up by setting this flag. By
     * default, repairing the graph after node removals creates more connections to improve the graph's quality. The
     * extra costs for this are relatively low (e.g. vs. regular indexing), and thus the default is recommended.
     */
    boolean reparationLimitCandidates() default false;

}
