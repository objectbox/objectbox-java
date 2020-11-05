package io.objectbox.sync.listener;

/**
 * Listens to sync connection events.
 */
public interface SyncConnectionListener {

    /**
     * Called when the client is disconnected from the sync server, e.g. due to a network error.
     * <p>
     * Depending on the configuration, the sync client typically tries to reconnect automatically,
     * triggering a {@link SyncLoginListener} again.
     */
    void onDisconnected();

}
