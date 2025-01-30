/*
 * Copyright 2019-2024 ObjectBox Ltd. All rights reserved.
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import io.objectbox.BoxStore;
import io.objectbox.annotation.apihint.Internal;
import io.objectbox.exception.FeatureNotAvailableException;
import io.objectbox.sync.internal.Platform;
import io.objectbox.sync.listener.SyncChangeListener;
import io.objectbox.sync.listener.SyncCompletedListener;
import io.objectbox.sync.listener.SyncConnectionListener;
import io.objectbox.sync.listener.SyncListener;
import io.objectbox.sync.listener.SyncLoginListener;
import io.objectbox.sync.listener.SyncTimeListener;

/**
 * A builder to create a {@link SyncClient}; the builder itself should be created via
 * {@link Sync#client(BoxStore, String, SyncCredentials)}.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public final class SyncBuilder {

    final Platform platform;
    final BoxStore boxStore;
    String url;
    final List<SyncCredentials> credentials;

    @Nullable SyncLoginListener loginListener;
    @Nullable SyncCompletedListener completedListener;
    @Nullable SyncChangeListener changeListener;
    @Nullable SyncConnectionListener connectionListener;
    @Nullable SyncTimeListener timeListener;
    @Nullable SyncListener listener;

    @Nullable
    String[] trustedCertPaths;
    boolean uncommittedAcks;

    RequestUpdatesMode requestUpdatesMode = RequestUpdatesMode.AUTO;

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


    @Internal
    public SyncBuilder(BoxStore boxStore, SyncCredentials credentials) {
        checkNotNull(boxStore, "BoxStore is required.");
        checkNotNull(credentials, "Sync credentials are required.");
        checkSyncFeatureAvailable();
        this.platform = Platform.findPlatform();
        this.boxStore = boxStore;
        this.credentials = Collections.singletonList(credentials);
    }

    @Internal
    public SyncBuilder(BoxStore boxStore, SyncCredentials[] multipleCredentials) {
        checkNotNull(boxStore, "BoxStore is required.");
        if (multipleCredentials.length == 0) {
            throw new IllegalArgumentException("At least one Sync credential is required.");
        }
        checkSyncFeatureAvailable();
        this.platform = Platform.findPlatform();
        this.boxStore = boxStore;
        this.credentials = Arrays.asList(multipleCredentials);
    }

    @Internal
    public SyncBuilder(BoxStore boxStore, String url, SyncCredentials credentials) {
        this(boxStore, credentials);
        checkNotNull(url, "Sync server URL is required.");
        this.url = url;
    }

    @Internal
    public SyncBuilder(BoxStore boxStore, String url, SyncCredentials[] multipleCredentials) {
        this(boxStore, multipleCredentials);
        checkNotNull(url, "Sync server URL is required.");
        this.url = url;
    }

    /**
     * Allows internal code to set the Sync server URL after creating this builder.
     */
    @Internal
    SyncBuilder serverUrl(String url) {
        this.url = url;
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
        checkNotNull(url, "Sync Server URL is required.");
        return new SyncClientImpl(this);
    }

    /**
     * Builds, {@link SyncClient#start() starts} and returns a Sync client.
     */
    public SyncClient buildAndStart() {
        SyncClient syncClient = build();
        syncClient.start();
        return syncClient;
    }

    private void checkNotNull(Object object, String message) {
        //noinspection ConstantConditions Non-null annotation does not enforce, so check for null.
        if (object == null) {
            throw new IllegalArgumentException(message);
        }
    }

}
