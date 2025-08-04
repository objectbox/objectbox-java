/*
 * Copyright 2019-2021 ObjectBox Ltd.
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

package io.objectbox.sync;

import io.objectbox.annotation.apihint.Beta;
import io.objectbox.sync.listener.SyncChangeListener;

// Note: this class is expected to be in this package by JNI, check before modifying/removing it.

/**
 * A collection of changes made to one entity type during a sync transaction.
 * Delivered via {@link SyncChangeListener}.
 * IDs of changed objects are available via {@link #getChangedIds()} and those of removed objects via
 * {@link #getRemovedIds()}.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
@Beta
public class SyncChange {
    final int entityTypeId;

    final long[] changedIds;
    final long[] removedIds;

    // Note: this constructor is called by JNI, check before modifying/removing it.
    public SyncChange(int entityTypeId, long[] changedIds, long[] removedIds) {
        this.entityTypeId = entityTypeId;
        this.changedIds = changedIds;
        this.removedIds = removedIds;
    }

    // Old version called by JNI, remove after some grace period.
    @Deprecated
    public SyncChange(long entityTypeId, long[] changedIds, long[] removedIds) {
        this.entityTypeId = (int) entityTypeId;
        this.changedIds = changedIds;
        this.removedIds = removedIds;
    }

    /**
     * The entity type ID; use methods like {@link io.objectbox.BoxStore#getEntityTypeIdOrThrow} to map with classes.
     */
    public int getEntityTypeId() {
        return entityTypeId;
    }

    /**
     * IDs of objects that have been changed; e.g. have been put/updated/inserted.
     */
    public long[] getChangedIds() {
        return changedIds;
    }

    /**
     * IDs of objects that have been removed.
     */
    public long[] getRemovedIds() {
        return removedIds;
    }
}
