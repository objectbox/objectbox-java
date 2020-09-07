package io.objectbox.sync.listener;

/**
 * Listens to sync completed events.
 */
public interface SyncCompletedListener {

    /**
     * Called each time a sync was "completed", in the sense that the client caught up with the current server state.
     * The client is "up-to-date".
     */
    void onUpdatesCompleted();

}
