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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.annotation.Nullable;

import io.objectbox.BoxStore;
import io.objectbox.exception.FeatureNotAvailableException;
import io.objectbox.sync.internal.Platform;
import io.objectbox.sync.listener.SyncChangeListener;
import io.objectbox.sync.listener.SyncCompletedListener;
import io.objectbox.sync.listener.SyncConnectionListener;
import io.objectbox.sync.listener.SyncListener;
import io.objectbox.sync.listener.SyncLoginListener;
import io.objectbox.sync.listener.SyncTimeListener;

/**
 * A builder to create a {@link SyncClient}; the builder itself should be created via {@link Sync#client(BoxStore)}.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public final class SyncBuilder {

    final Platform platform;
    final BoxStore boxStore;
    final List<String> urls = new ArrayList<>();
    final List<SyncCredentials> credentials = new ArrayList<>();

    @Nullable SyncLoginListener loginListener;
    @Nullable SyncCompletedListener completedListener;
    @Nullable SyncChangeListener changeListener;
    @Nullable SyncConnectionListener connectionListener;
    @Nullable SyncTimeListener timeListener;
    @Nullable SyncListener listener;

    @Nullable
    String[] trustedCertPaths;
    int flags;
    boolean uncommittedAcks;

    RequestUpdatesMode requestUpdatesMode = RequestUpdatesMode.AUTO;
    // To be helpful when debugging, use a TreeMap so variables are eventually passed ordered by name to the native API
    final Map<String, String> filterVariables = new TreeMap<>();

    public enum RequestUpdatesMode {
        /**
         * Once logged in, does not request any sync updates automatically.
         * <p>
         * Sync updates will have to be requested manually.
         *
         * @see SyncClient#requestUpdates()
         * @see SyncClient#requestUpdatesOnce()
         */
        MANUAL,

        /**
         * Once logged in, requests sync updates automatically including subsequent pushes for data changes.
         * This is the default.
         */
        AUTO,

        /**
         * Once logged in, requests updates automatically once without subsequent pushes for data changes.
         * <p>
         * After the initial sync update, further updates will have to be requested manually.
         *
         * @see SyncClient#requestUpdates()
         * @see SyncClient#requestUpdatesOnce()
         */
        AUTO_NO_PUSHES
    }

    private static void checkSyncFeatureAvailable() {
        if (!BoxStore.isSyncAvailable()) {
            throw new FeatureNotAvailableException(
                    "This library does not include ObjectBox Sync. " +
                            "Please visit https://objectbox.io/sync/ for options.");
        }
    }

    /**
     * Creates a builder for a {@link SyncClient}.
     * <p>
     * Don't use this directly, use the {@link Sync#client} method instead.
     */
    SyncBuilder(BoxStore boxStore) {
        checkNotNull(boxStore, "boxStore");
        this.boxStore = boxStore;
        checkSyncFeatureAvailable();
        this.platform = Platform.findPlatform(); // Requires APIs only present in Android Sync library
    }

    /**
     * Adds a Sync server URL the client should connect to.
     * <p>
     * This is typically a WebSockets URL starting with {@code ws://} or {@code wss://} (for encrypted connections), for
     * example if the server is running on localhost {@code ws://127.0.0.1:9999}.
     * <p>
     * Can be called multiple times to add multiple URLs for high availability and load balancing (like when using an
     * ObjectBox Sync Server Cluster). A random URL is selected for each connection attempt.
     *
     * @param url The URL of the Sync server on which the Sync protocol is exposed.
     * @return this builder for chaining
     * @see #urls(List)
     */
    public SyncBuilder url(String url) {
        checkNotNull(url, "url");
        this.urls.add(url);
        return this;
    }

    /**
     * Like {@link #url(String)}, but accepts a list of URLs.
     *
     * @param urls A list of URLs of Sync servers on which the Sync protocol is exposed.
     * @return this builder for chaining
     * @see #url(String)
     */
    public SyncBuilder urls(List<String> urls) {
        checkNotNull(urls, "urls");
        for (String url : urls) {
            url(url);
        }
        return this;
    }

    /**
     * Adds {@link SyncCredentials} to authenticate the client with the server.
     * <p>
     * The accepted credentials types depend on your Sync server configuration.
     *
     * @param credentials credentials created using a {@link SyncCredentials} factory method, for example
     * {@code SyncCredentials.jwtIdToken(idToken)}.
     * @see #credentials(List)
     */
    public SyncBuilder credentials(SyncCredentials credentials) {
        checkNotNull(credentials, "credentials");
        this.credentials.add(credentials);
        return this;
    }

    /**
     * Like {@link #credentials(SyncCredentials)}, but accepts a list of credentials.
     *
     * @param credentials a list of credentials where each element is created using a {@link SyncCredentials} factory
     * method, for example {@code SyncCredentials.jwtIdToken(idToken)}.
     * @return this builder for chaining
     */
    public SyncBuilder credentials(List<SyncCredentials> credentials) {
        checkNotNull(credentials, "credentials");
        for (SyncCredentials credential : credentials) {
            credentials(credential);
        }
        return this;
    }

    /**
     * Adds or replaces a <a href="https://sync.objectbox.io/sync-server/sync-filters">Sync filter</a> variable value
     * for the given name.
     * <p>
     * Sync client filter variables can be used in server-side Sync filters to filter out objects that do not match the
     * filter.
     *
     * @see SyncClient#putFilterVariable
     */
    public SyncBuilder filterVariable(String name, String value) {
        checkNotNull(name, "name");
        checkNotNull(value, "value");
        filterVariables.put(name, value);
        return this;
    }

    /**
     * Configures a custom set of directory or file paths to search for trusted certificates in.
     * The first path that exists will be used.
     * <p>
     * Using this option is not recommended in most cases, as by default the sync client uses
     * the certificate authorities trusted by the host platform.
     */
    public SyncBuilder trustedCertificates(String[] paths) {
        // Copy to prevent external modification.
        this.trustedCertPaths = Arrays.copyOf(paths, paths.length);
        return this;
    }

    /**
     * Sets bit flags to adjust Sync behavior, like additional logging.
     *
     * @param flags One or multiple {@link SyncFlags}, combined with bitwise or.
     */
    public SyncBuilder flags(int flags) {
        this.flags = flags;
        return this;
    }

    /**
     * Configure automatic sync updates from the server.
     * If automatic sync updates are turned off, they will need to be requested using the sync client.
     *
     * @see SyncClient#requestUpdates()
     * @see SyncClient#requestUpdatesOnce()
     */
    public SyncBuilder requestUpdatesMode(RequestUpdatesMode requestUpdatesMode) {
        this.requestUpdatesMode = requestUpdatesMode;
        return this;
    }

    /**
     * Turns on sending of uncommitted acks.
     */
    public SyncBuilder uncommittedAcks() {
        this.uncommittedAcks = true;
        return this;
    }

    /**
     * Sets a listener to only observe Sync login events.
     * <p>
     * This listener can also be {@link SyncClient#setSyncLoginListener(SyncLoginListener) set or removed}
     * on the Sync client directly.
     */
    public SyncBuilder loginListener(SyncLoginListener loginListener) {
        this.loginListener = loginListener;
        return this;
    }

    /**
     * Sets a listener to only observe Sync completed events.
     * <p>
     * This listener can also be {@link SyncClient#setSyncCompletedListener(SyncCompletedListener) set or removed}
     * on the Sync client directly.
     */
    public SyncBuilder completedListener(SyncCompletedListener completedListener) {
        this.completedListener = completedListener;
        return this;
    }

    /**
     * Sets a listener to observe fine granular changes happening during sync.
     * <p>
     * This listener can also be {@link SyncClient#setSyncChangeListener(SyncChangeListener) set or removed}
     * on the Sync client directly.
     */
    public SyncBuilder changeListener(SyncChangeListener changeListener) {
        this.changeListener = changeListener;
        return this;
    }

    /**
     * Sets a listener to only observe Sync time events.
     * <p>
     * This listener can also be {@link SyncClient#setSyncTimeListener(SyncTimeListener) set or removed}
     * on the Sync client directly.
     */
    public SyncBuilder timeListener(SyncTimeListener timeListener) {
        this.timeListener = timeListener;
        return this;
    }

    /**
     * Sets a listener to only observe Sync connection events.
     * <p>
     * This listener can also be {@link SyncClient#setSyncConnectionListener(SyncConnectionListener) set or removed}
     * on the Sync client directly.
     */
    public SyncBuilder connectionListener(SyncConnectionListener connectionListener) {
        this.connectionListener = connectionListener;
        return this;
    }

    /**
     * Sets a listener to observe all Sync events like login or sync completion.
     * <p>
     * Note: this will replace any login, completed or connection listener.
     * <p>
     * This listener can also be {@link SyncClient#setSyncListener(SyncListener) set or removed}
     * on the Sync client directly.
     */
    public SyncBuilder listener(SyncListener listener) {
        this.listener = listener;
        return this;
    }

    /**
     * Builds and returns a Sync client ready to {@link SyncClient#start()}.
     */
    public SyncClient build() {
        if (boxStore.getSyncClient() != null) {
            throw new IllegalStateException("The given store is already associated with a Sync client, close it first.");
        }
        return new SyncClientImpl(this);
    }

    /**
     * {@link #build() Builds}, {@link SyncClient#start() starts} and returns a Sync client.
     */
    public SyncClient buildAndStart() {
        SyncClient syncClient = build();
        syncClient.start();
        return syncClient;
    }

    /**
     * Nullness annotations are only a hint in Java, so explicitly check nonnull annotated parameters
     * (see package-info.java for package settings).
     */
    private void checkNotNull(@Nullable Object object, String name) {
        if (object == null) {
            throw new IllegalArgumentException(name + " must not be null.");
        }
    }

}
