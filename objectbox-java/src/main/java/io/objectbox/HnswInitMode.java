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
 * What {@link BoxStore#initHnswIndex(Property, HnswIndexParams, HnswInitMode)} does with the existing objects of the
 * type.
 * <p>
 * Caution with Sync: for a sync-enabled type, the removing modes remove the objects like other changes do, so if a
 * sync client is running, the removals are synced to the server (and thus to other clients).
 */
public enum HnswInitMode {

    // Note: the ordinal is the value for the native API; do not reorder.

    /**
     * Keeps the objects: indexes them if needed (the first init or other parameters requiring re-indexing).
     * <p>
     * Objects whose vectors do not fit the parameters (fewer dimensions) are kept, but not indexed (and not counted);
     * consider {@link #REMOVE_ALL_OBJECTS_ON_REINDEX} for those.
     */
    KEEP_OBJECTS,

    /**
     * Removes all objects of the type instead of indexing them (the first init or other parameters requiring
     * re-indexing), e.g. if their vectors would not fit the parameters anyway (other dimensions or embedding model);
     * keeps them otherwise, e.g. with the parameters in effect.
     * <p>
     * Removing all objects at once is much cheaper than indexing them (and than removing them one by one).
     */
    REMOVE_ALL_OBJECTS_ON_REINDEX,

    /**
     * Always removes all objects of the type, also with the parameters in effect.
     */
    REMOVE_ALL_OBJECTS_ALWAYS
}
