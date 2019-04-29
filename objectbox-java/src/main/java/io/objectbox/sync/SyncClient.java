package io.objectbox.sync;

/** Public sync client API. SyncClient is thread-safe. */
@SuppressWarnings("unused")
public interface SyncClient {

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
    void connect(ConnectCallback callback);

    /**
     * Disconnects from the sync server and stops syncing.
     */
    void disconnect();

}
