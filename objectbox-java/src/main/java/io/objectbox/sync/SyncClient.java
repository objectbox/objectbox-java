/*
 * Copyright 2019-2025 ObjectBox Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.objectbox.sync;

import java.io.Closeable;
import java.util.List;

import javax.annotation.Nullable;

import io.objectbox.annotation.apihint.Experimental;
import io.objectbox.sync.SyncBuilder.RequestUpdatesMode;
import io.objectbox.sync.listener.SyncChangeListener;
import io.objectbox.sync.listener.SyncCompletedListener;
import io.objectbox.sync.listener.SyncConnectionListener;
import io.objectbox.sync.listener.SyncListener;
import io.objectbox.sync.listener.SyncLoginListener;
import io.objectbox.sync.listener.SyncTimeListener;

/**
 * ObjectBox sync client. Build a client with {@link Sync#client}.
 * <p>
 * Keep the instance around (avoid garbage collection) while you want to have sync ongoing.
 * For a clean shutdown, call {@link #close()}.
 * <p>
 * SyncClient is thread-safe.
 */
@SuppressWarnings("unused")
public interface SyncClient extends Closeable {

    /**
     * Gets the sync server URL this client is connected to.
     *
     * @deprecated Use {@link #getUrls()}
     */
    @Deprecated
    String getServerUrl();

    /**
     * Gets the sync server URLs this client may connect to.
     * <p>
     * See {@link SyncBuilder#url(String)} for notes on multiple URLs.
     */
    List<String> getUrls();

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
     * Estimates the current server timestamp in nanoseconds based on the last known server time.
     *
     * @return unix timestamp in nanoseconds (since epoch);
     * or 0 if there has not been a server contact yet and thus the server's time is unknown
     */
    long getServerTimeNanos();

    /**
     * Returns the estimated difference in nanoseconds between the server time and the local timestamp.
     * urns the difference in nanoseconds between the current local time of this client
     * Equivalent to calculating {@link #getServerTimeNanos()} - "current time" (nanos since epoch),
     * except for when the server time is unknown, then the result is zero.
     *
     * @return time difference in nanoseconds; e.g. positive if server time is ahead of local time;
     * or 0 if there has not been a server contact yet and thus the server's time is unknown
     */
    long getServerTimeDiffNanos();

    /**
     * Returns the estimated roundtrip time in nanoseconds to the server and back.
     * This is measured during login.
     *
     * @return roundtrip time in nanoseconds;
     * or 0 if there has not been a server contact yet and thus the roundtrip time could not be estimated
     */
    long getRoundtripTimeNanos();

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
     * Sets a {@link SyncTimeListener}. Replaces a previously set listener.
     * Set to {@code null} to remove the listener.
     */
    void setSyncTimeListener(@Nullable SyncTimeListener timeListener);

    /**
     * Adds or replaces a <a href="https://sync.objectbox.io/sync-server/sync-filters">Sync filter</a> variable value
     * for the given name.
     * <p>
     * Eventually, existing values for the same name are replaced.
     * <p>
     * Sync client filter variables can be used in server-side Sync filters to filter out objects that do not match the
     * filter. Filter variables must be added before login, so before calling {@link #start()}.
     *
     * @see #removeFilterVariable
     * @see #removeAllFilterVariables
     */
    void putFilterVariable(String name, String value);

    /**
     * Removes a previously added Sync filter variable value.
     *
     * @see #putFilterVariable
     * @see #removeAllFilterVariables
     */
    void removeFilterVariable(String name);

    /**
     * Removes all previously added Sync filter variable values.
     *
     * @see #putFilterVariable
     * @see #removeFilterVariable
     */
    void removeAllFilterVariables();

    /**
     * Sets credentials to authenticate the client with the server.
     * <p>
     * Any credentials that were set before are replaced.
     * <p>
     * Usually, credentials are passed via {@link SyncBuilder#credentials(SyncCredentials)}, but this can be used to
     * update them later, such as when a token expires.
     * <p>
     * The accepted credentials type depends on your Sync server configuration.
     *
     * @param credentials credentials created using a {@link SyncCredentials} factory method, for example
     * {@code SyncCredentials.jwtIdToken(idToken)}.
     * @see #setLoginCredentials(List)
     */
    void setLoginCredentials(SyncCredentials credentials);

    /**
     * Like {@link #setLoginCredentials(SyncCredentials)}, but accepts a list of credentials.
     *
     * @param credentials a list of credentials where each element is created using a {@link SyncCredentials} factory
     * method, for example {@code SyncCredentials.jwtIdToken(idToken)}.
     */
    void setLoginCredentials(List<SyncCredentials> credentials);

    /**
     * Like {@link #setLoginCredentials(SyncCredentials)}, but accepts an array of credentials.
     *
     * @param multipleCredentials an array of credentials where each element is created using a {@link SyncCredentials}
     * factory method, for example {@code SyncCredentials.jwtIdToken(idToken)}.
     */
    void setLoginCredentials(SyncCredentials[] multipleCredentials);

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
     * Stops the client. Does nothing if the sync client is already stopped.
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
     * @return 'true' if the request was likely sent (e.g. the sync client is in "logged in" state)
     * or 'false' if the request was not sent (and will not be sent in the future)
     * @see #cancelUpdates()
     */
    boolean requestUpdates();

    /**
     * Asks the server to send sync updates until this sync client is up-to-date, then pauses sync updates again.
     * This is useful if sync updates were turned off with
     * {@link SyncBuilder#requestUpdatesMode(RequestUpdatesMode) requestUpdatesMode(MANUAL)}.
     *
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
     * Temporary only, try not to use it.
     * <p>
     * Request a sync of all previous changes from the server.
     *
     * @return 'true' if the request was likely sent (e.g. the sync client is in "logged in" state)
     * or 'false' if the request was not sent (and will not be sent in the future).
     */
    @Experimental
    boolean requestFullSync();

    /**
     * Lets the sync client know that a working network connection
     * is available.
     * <p>
     * This can help speed up reconnecting to the sync server.
     */
    void notifyConnectionAvailable();

    /**
     * Experimental. This API might change or be removed in the future.
     * <p>
     * Start building a message of Objects with optional flags (set to 0) and topic (set to null).
     * <p>
     * Use like
     * <pre>
     * syncClient.startObjectsMessage(0, "some-topic")
     *     .addString(1, "Hello!")
     *     .addBytes(2, "Hello!".getBytes(StandardCharsets.UTF_8), false)
     *     .send();
     * </pre>
     */
    @Experimental
    ObjectsMessageBuilder startObjectsMessage(long flags, @Nullable String topic);

}
