package io.objectbox.sync;

/**
 * A collection of changes made to one entity type during a sync transaction.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class SyncChange {
    final long entityTypeId;

    final long[] changedIds;
    final long[] removedIds;

    // note: this constructor is called by JNI, check before modifying/removing it
    public SyncChange(long entityTypeId, long[] changedIds, long[] removedIds) {
        this.entityTypeId = entityTypeId;
        this.changedIds = changedIds;
        this.removedIds = removedIds;
    }

    public long getEntityTypeId() {
        return entityTypeId;
    }

    public long[] getChangedIds() {
        return changedIds;
    }

    public long[] getRemovedIds() {
        return removedIds;
    }
}
