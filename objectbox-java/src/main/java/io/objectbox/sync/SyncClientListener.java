package io.objectbox.sync;

@SuppressWarnings({"unused"})
public interface SyncClientListener {

    /**
     * Called on a successful login.
     */
    void onLogin();

    /**
     * Called on a login failure. One of {@link SyncLoginCodes}, but never {@link SyncLoginCodes#OK}.
     */
    void onLoginFailure(long response);

    /**
     * Called each time a sync was completed.
     */
    void onSyncComplete();

    /**
     * Called when the client is disconnected from the sync server, e.g. due to a network error.
     */
    void onDisconnect();

}
