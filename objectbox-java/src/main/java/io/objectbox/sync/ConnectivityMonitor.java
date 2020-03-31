package io.objectbox.sync;

import javax.annotation.Nullable;

/**
 * Used by {@link SyncClient} to observe connectivity changes.
 * <p>
 * Implementations are provided by a {@link io.objectbox.sync.internal.Platform platform}.
 */
public abstract class ConnectivityMonitor {

    @Nullable
    private SyncClient syncClient;

    void setObserver(SyncClient syncClient) {
        //noinspection ConstantConditions Annotations do not enforce non-null.
        if (syncClient == null) {
            throw new IllegalArgumentException("Sync client must not be null");
        }
        this.syncClient = syncClient;
        onObserverSet();
    }

    void removeObserver() {
        this.syncClient = null;
        onObserverRemoved();
    }

    /**
     * Called right after the observer was set.
     */
    public void onObserverSet() {
    }

    /**
     * Called right after the observer was removed.
     */
    public void onObserverRemoved() {
    }

    /**
     * Notifies the observer that a connection is available.
     * Implementers should call this once a working network connection is available.
     */
    public final void notifyConnectionAvailable() {
        SyncClient syncClient = this.syncClient;
        if (syncClient != null) {
            syncClient.notifyConnectionAvailable();
        }
    }

}
