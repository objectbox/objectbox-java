package io.objectbox.sync;

import io.objectbox.annotation.apihint.Experimental;

import java.io.Closeable;

/**
 * ObjectBox sync client. Build a client with {@link Sync#client}.
 *
 * Keep the instance around (avoid garbage collection) while you want to have sync ongoing.
 * For a clean shutdown, call {@link #close()}.
 * <p>
 * SyncClient is thread-safe.
 */
@SuppressWarnings("unused")
@Experimental
public interface SyncClient extends Closeable {

    /**
     * Gets the sync server URL this client is connected to.
     */
    String getServerUrl();

    /**
     * Flag indicating if the sync client was started.
     * Started clients try to connect, login, and sync with the sync destination.
     */
    boolean isStarted();

    /**
     * Flag indicating if the sync client was started.
     * Logged in clients can sync with the sync destination to exchange data.
     */
    boolean isLoggedIn();

    /**
     * Response code of last login attempt. One of {@link SyncLoginCodes}.
     */
    long getLastLoginCode();

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
     * Updates the login credentials. This should not be required during regular use.
     * The original credentials were passed when building sync client.
     */
    void setLoginCredentials(SyncCredentials credentials);

    /**
     * Waits until the sync client receives a response to its first (connection and) login attempt
     * or until the given time has expired.
     * Use {@link #isLoggedIn()} or {@link #getLastLoginCode()} afterwards to determine if login was successful.
     * Starts the sync if it is not already.
     *
     * @return true if a response was received in the given time window.
     */
    boolean awaitFirstLogin(long millisToWait);

    /**
     * Starts the client. It will connect to the server, log in (authenticate) and start syncing.
     */
    void start();

    /**
     * Stops the client. Does nothing if the sync client is already stopped or closed.
     */
    void stop();

    /**
     * Closes and cleans up all resources used by this sync client.
     * It can no longer be used afterwards, build a new sync client instead.
     * Does nothing if this sync client has already been closed.
     */
    void close();

    /**
     * Asks the sync server to resume sync updates.
     * This requires that the sync client was built with {@link SyncBuilder#manualUpdateRequests} set.
     *
     * @see #cancelUpdates()
     */
    void requestUpdates();

    /**
     * Asks the server to send sync updates until this sync client is up-to-date, then pauses sync updates again.
     * This requires that the sync client was built with {@link SyncBuilder#manualUpdateRequests} set.
     */
    void requestUpdatesOnce();

    /**
     * Asks the server to pause sync updates.
     *
     * @see #requestUpdates()
     */
    void cancelUpdates();

    /**
     * Experimental. This API might change or be removed in the future.
     * <p>
     * Request a sync of all previous changes from the server.
     */
    @Experimental
    void requestFullSync();

}
