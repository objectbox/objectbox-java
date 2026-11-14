/*
 * Copyright 2026 ObjectBox Ltd. <https://objectbox.io>
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
     * No longer has any effect: since 6.0, removing objects is fast without it.
     *
     * @deprecated Ignored since 6.0; just remove it.
     */
    @Deprecated
    boolean reparationLimitCandidates() default false;

    /**
     * Late init: for vectors whose dimensions (and other index parameters) are only known at runtime, e.g. if the
     * embedding model is chosen at runtime.
     * <p>
     * With this flag, the {@link HnswIndex#dimensions()} are optional; without them, the index is not initialized:
     * putting objects of the type throws, while reading them works. Set the index parameters after opening the store,
     * before the index is used (e.g. before putting objects of the type), with {@code BoxStore.initHnswIndex()}.
     * The parameters are persisted, so later sessions use them, and can be changed later (e.g. for another embedding
     * model).
     */
    boolean lateInit() default false;

}
