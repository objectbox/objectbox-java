package io.objectbox.sync.listener;

import io.objectbox.annotation.apihint.Experimental;
import io.objectbox.sync.SyncChange;

/**
 * Notifies of fine granular changes on the object level happening during sync.
 * Register your listener using {@link io.objectbox.sync.SyncBuilder#changeListener(SyncChangeListener) SyncBuilder.changesListener(SyncChangesListener)}.
 * Note that enabling fine granular notification can slightly reduce performance.
 * <p>
 * See also {@link SyncListener} for the general sync listener.
 */
@SuppressWarnings({"unused"})
@Experimental
public interface SyncChangeListener {

    // Note: this method is expected by JNI, check before modifying/removing it.

    /**
     * Called each time when data from sync was applied locally.
     *
     * @param syncChanges This contains the entity type (schema) ID, the removed IDs and the put IDs for that entity.
     */
    void onSyncChanges(SyncChange[] syncChanges);

}
