package io.objectbox.sync;

import io.objectbox.annotation.apihint.Experimental;

/**
 * Notifies of fine granular changes on the object level happening during sync.
 * Register your listener using {@link SyncBuilder#changesListener(SyncChangesListener)}.
 * Note that enabling fine granular notification can slightly reduce performance.
 * <p>
 * See also {@link SyncClientListener} for the general sync listener.
 */
@SuppressWarnings({"unused"})
@Experimental
public interface SyncChangesListener {

    /**
     * Called each time when data from sync was applied locally.
     *
     * @param syncChanges This contains the entity type (schema) ID, the removed IDs and the put IDs for that entity.
     */
    void onSyncChanges(SyncChange[] syncChanges);

}
