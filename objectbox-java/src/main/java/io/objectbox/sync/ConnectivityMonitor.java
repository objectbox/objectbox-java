package io.objectbox.sync;

import javax.annotation.Nullable;

/**
 * Used by {@link SyncClient} to observe connectivity changes.
 * <p>
 * Instead of this class, use the platform-specific implementations.
 */
public abstract class ConnectivityMonitor {

    @Nullable
    private SyncClient syncClient;

    void setObserver(SyncClient syncClient) {
        this.syncClient = syncClient;
    }

    void removeObserver() {
        this.syncClient = null;
    }

    /**
     * Called if a working network connection is available.
     */
    public void onConnectionAvailable() {
        SyncClient syncClient = this.syncClient;
        if (syncClient != null) {
            syncClient.notifyConnectionAvailable();
        }
    }

}
