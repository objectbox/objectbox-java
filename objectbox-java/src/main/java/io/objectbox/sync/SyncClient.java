package io.objectbox.sync;

import io.objectbox.annotation.apihint.Experimental;
import io.objectbox.annotation.apihint.Temporary;
import io.objectbox.sync.SyncBuilder.RequestUpdatesMode;
import io.objectbox.sync.listener.SyncChangeListener;
import io.objectbox.sync.listener.SyncCompletedListener;
import io.objectbox.sync.listener.SyncConnectionListener;
import io.objectbox.sync.listener.SyncListener;
import io.objectbox.sync.listener.SyncLoginListener;

import java.io.Closeable;

import javax.annotation.Nullable;

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
     * Sets a listener to observe login events. Replaces a previously set listener.
     * Set to {@code null} to remove the listener.
     */
    void setSyncLoginListener(@Nullable SyncLoginListener listener);

    /**
     * Sets a listener to observe Sync completed events. Replaces a previously set listener.
     * Set to {@code null} to remove the listener.
     */
    void setSyncCompletedListener(@Nullable SyncCompletedListener listener);

    /**
     * Sets a listener to observe Sync connection events. Replaces a previously set listener.
     * Set to {@code null} to remove the listener.
     */
    void setSyncConnectionListener(@Nullable SyncConnectionListener listener);

    /**
     * Sets a listener to observe all Sync events.
     * Replaces all other previously set listeners, except a {@link SyncChangeListener}.
     * Set to {@code null} to remove the listener.
     */
    void setSyncListener(@Nullable SyncListener listener);

    /**
     * Sets a {@link SyncChangeListener}. Replaces a previously set listener.
     * Set to {@code null} to remove the listener.
     */
    void setSyncChangeListener(@Nullable SyncChangeListener listener);

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
     * This is useful if sync updates were turned off with
     * {@link SyncBuilder#requestUpdatesMode(RequestUpdatesMode) requestUpdatesMode(MANUAL)}.
     *
     * @see #cancelUpdates()
     * @return 'true' if the request was likely sent (e.g. the sync client is in "logged in" state)
     * or 'false' if the request was not sent (and will not be sent in the future)
     */
    boolean requestUpdates();

    /**
     * Asks the server to send sync updates until this sync client is up-to-date, then pauses sync updates again.
     * This is useful if sync updates were turned off with
     * {@link SyncBuilder#requestUpdatesMode(RequestUpdatesMode) requestUpdatesMode(MANUAL)}.
     * @return 'true' if the request was likely sent (e.g. the sync client is in "logged in" state)
     * or 'false' if the request was not sent (and will not be sent in the future)
     */
    boolean requestUpdatesOnce();

    /**
     * Asks the server to pause sync updates.
     *
     * @return 'true' if the request was likely sent (e.g. the sync client is in "logged in" state)
     * or 'false' if the request was not sent (and will not be sent in the future)
     * @see #requestUpdates()
     */
    boolean cancelUpdates();

    /**
     * Experimental. This API might change or be removed in the future.
     * <p>
     * Request a sync of all previous changes from the server.
     * @return 'true' if the request was likely sent (e.g. the sync client is in "logged in" state)
     * or 'false' if the request was not sent (and will not be sent in the future)
     */
    @Temporary
    boolean requestFullSync();

    /**
     * Lets the sync client know that a working network connection
     * is available.
     * <p>
     * This can help speed up reconnecting to the sync server.
     */
    void notifyConnectionAvailable();

}
