package io.objectbox.sync;

import io.objectbox.annotation.apihint.Experimental;

/**
 * A collection of changes made to one entity type during a sync transaction.
 * Delivered via {@link SyncChangesListener}.
 * IDs of changed objects are available via {@link #getChangedIds()} and those of removed objects via
 * {@link #getRemovedIds()}.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
@Experimental
public class SyncChange {
    final long entityTypeId;

    final long[] changedIds;
    final long[] removedIds;

    // Note: this constructor is called by JNI, check before modifying/removing it.
    public SyncChange(long entityTypeId, long[] changedIds, long[] removedIds) {
        this.entityTypeId = entityTypeId;
        this.changedIds = changedIds;
        this.removedIds = removedIds;
    }

    /**
     * The entity type ID; use methods like {@link io.objectbox.BoxStore#getEntityTypeIdOrThrow} to map with classes.
     */
    public long getEntityTypeId() {
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
