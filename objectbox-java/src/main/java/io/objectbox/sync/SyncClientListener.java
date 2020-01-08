package io.objectbox.sync;

import io.objectbox.annotation.apihint.Experimental;

/**
 * This listener has callback methods invoked by fundamental synchronization events.
 * Set via {@link SyncBuilder#listener(SyncClientListener)} or {@link SyncClient#setSyncListener(SyncClientListener)}.
 */
@SuppressWarnings({"unused"})
@Experimental
public interface SyncClientListener {

    /**
     * Called on a successful login. At this point the connection to the sync destination was established and
     * entered an operational state, in which data can be sent both ways.
     */
    void onLogin();

    /**
     * Called on a login failure. One of {@link SyncLoginCodes}, but never {@link SyncLoginCodes#OK}.
     */
    void onLoginFailure(long response);

    /**
     * Called each time a sync was "completed", in the sense that the client caught up with the current server state.
     * The client is "up-to-date".
     */
    void onSyncComplete();

    /**
     * Called when the client is disconnected from the sync server, e.g. due to a network error.
     * Depending on the configuration, the sync client typically tries to reconnect automatically, triggering
     * {@link #onLogin()} (or {@link #onLoginFailure(long)}) again.
     */
    void onDisconnect();

}
