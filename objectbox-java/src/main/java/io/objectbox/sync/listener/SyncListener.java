package io.objectbox.sync.listener;

import io.objectbox.annotation.apihint.Experimental;
import io.objectbox.sync.SyncBuilder;
import io.objectbox.sync.SyncClient;

/**
 * This listener has callback methods invoked by all fundamental synchronization events.
 * Set via {@link SyncBuilder#listener(SyncListener)} or {@link SyncClient#setSyncListener(SyncListener)}.
 * <p>
 * See {@link AbstractSyncListener} for a no-op convenience implementation.
 * <p>
 * Use more specific listeners, like {@link SyncLoginListener}, to only receive a sub-set of events.
 */
@Experimental
public interface SyncListener extends SyncLoginListener, SyncCompletedListener, SyncChangeListener, SyncConnectionListener {
}
