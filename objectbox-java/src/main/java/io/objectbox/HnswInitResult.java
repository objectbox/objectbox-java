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

package io.objectbox;

/**
 * What {@link BoxStore#initHnswIndex(Property, HnswIndexParams, HnswInitMode)} did; e.g. if it removed the objects,
 * the app may want to recompute their vectors.
 */
public final class HnswInitResult {

    // Bit flags of the native API
    private static final long PARAMS_CHANGED = 1;
    private static final long OBJECTS_INDEXED = 2;
    private static final long OBJECTS_REMOVED = 4;

    private final long flags;
    private final long objectCount;

    HnswInitResult(long flags, long objectCount) {
        this.flags = flags;
        this.objectCount = objectCount;
    }

    /**
     * If nothing changed, e.g. the parameters were already in effect.
     */
    public boolean isUnchanged() {
        return flags == 0;
    }

    /**
     * If the parameters were set (and persisted): the first init or other parameters.
     */
    public boolean isParamsChanged() {
        return (flags & PARAMS_CHANGED) != 0;
    }

    /**
     * If objects of the type were indexed; see {@link #getObjectCount()}.
     */
    public boolean isObjectsIndexed() {
        return (flags & OBJECTS_INDEXED) != 0;
    }

    /**
     * If objects of the type were removed (see {@link HnswInitMode}); see {@link #getObjectCount()}.
     */
    public boolean isObjectsRemoved() {
        return (flags & OBJECTS_REMOVED) != 0;
    }

    /**
     * The number of indexed objects (those with vectors fitting the dimensions) or removed objects.
     */
    public long getObjectCount() {
        return objectCount;
    }

    @Override
    public String toString() {
        return "HnswInitResult{paramsChanged=" + isParamsChanged()
                + ", objectsIndexed=" + isObjectsIndexed()
                + ", objectsRemoved=" + isObjectsRemoved()
                + ", objectCount=" + objectCount + "}";
    }
}
