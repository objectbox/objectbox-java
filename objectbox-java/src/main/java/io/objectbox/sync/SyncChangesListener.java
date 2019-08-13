package io.objectbox.sync;

/**
 * Notifies you of fine granular changes happening during sync.
 * Most will want to use {@link SyncClientListener} instead.
 */
@SuppressWarnings({"unused"})
public interface SyncChangesListener {

    /**
     * Called each time when data from sync was applied locally.
     *
     * @param syncChanges This contains the entity type (schema) ID, the removed IDs and the put IDs for that entity.
     */
    void onSyncChanges(SyncChange[] syncChanges);

}
