package io.objectbox.sync;

import io.objectbox.BoxStore;

import java.io.Closeable;

/**
 * Data synchronization client built with {@link Sync#with(BoxStore, String)}.
 *
 * SyncClient is thread-safe.
 */
@SuppressWarnings("unused")
public interface SyncClient extends Closeable {

    /** Just in case you need to update since calling {@link SyncBuilder#credentials(SyncCredentials)}. */
    void setLoginCredentials(SyncCredentials credentials);

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
     * Waits for the sync client to make its first (connection and) login attempt.
     * Check the actual outcome of the login using {@link #isLoggedIn()} and/or {@link #getLastLoginCode()}.
     *
     * @return true if we got a response to the first login attempt in time
     */
    boolean awaitFirstLogin(long millisToWait);

    /** Closes everything (e.g. deletes native resources); do not use this object afterwards. */
    void close();

    /** Starts the synchronization. */
    void start();

    /** Stops the synchronization. */
    void stop();

    boolean isStarted();

    long getLastLoginCode();

    boolean isLoggedIn();

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
