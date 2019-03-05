package io.objectbox.sync;

/** Public sync client API. */
public interface SyncClient {

    /** Get the sync server URL this client is connected to. */
    String url();

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
