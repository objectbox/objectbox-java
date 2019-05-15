package io.objectbox.sync;

import java.io.Closeable;

/** Public sync client API. SyncClient is thread-safe. */
@SuppressWarnings("unused")
public interface SyncClient extends Closeable {

    /** Get the sync server URL this client is connected to. */
    String url();

    /**
     * Sets a {@link SyncClientListener}. Replaces a previously set listener.
     */
    void setSyncListener(SyncClientListener listener);

    /**
     * Removes a previously set {@link SyncClientListener}. Does nothing if no listener was set.
     */
    void removeSyncListener();

    /**
     * Sets a {@link SyncChangesListener}. Replaces a previously set listener.
     */
    void setSyncChangesListener(SyncChangesListener listener);

    /**
     * Removes a previously set {@link SyncChangesListener}. Does nothing if no listener was set.
     */
    void removeSyncChangesListener();

    /**
     * Logs the client in with the sync server and starts or resumes syncing.
     * If successful no exception will be returned with the callback.
     */
    void awaitLogin(ConnectCallback callback);

    /** Closes everything (e.g. deletes native resources); do not use this object afterwards. */
    void close();

    /** Starts the synchronization. */
    void start();

    /** Stops the synchronization. */
    void stop();

    /**
     * In combination with {@link SyncBuilder#manualUpdateRequests}, this manually requests updates from the sync
     * backend including pushes of future changes.
     * Also resumes updates after {@link #cancelUpdates()} was called.
     */
    void requestUpdates();

    /**
     * In combination with {@link SyncBuilder#manualUpdateRequests}, this manually requests updates from the sync
     * backend until we are up-to-date once without pushes for future changes.
     */
    void requestUpdatesOnce();

    /** Stop receiving sync updates. */
    void cancelUpdates();

}
