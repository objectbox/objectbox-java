package io.objectbox.sync.listener;

import io.objectbox.sync.SyncLoginCodes;

/**
 * Listens to login events.
 */
public interface SyncLoginListener {

    /**
     * Called on a successful login.
     * <p>
     * At this point the connection to the sync destination was established and
     * entered an operational state, in which data can be sent both ways.
     */
    void onLoggedIn();

    /**
     * Called on a login failure. One of {@link SyncLoginCodes}, but never {@link SyncLoginCodes#OK}.
     */
    void onLoginFailed(long syncLoginCode);

}
