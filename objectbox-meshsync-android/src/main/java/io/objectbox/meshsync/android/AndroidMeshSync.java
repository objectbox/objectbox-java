/*
 * Copyright 2026 ObjectBox Ltd. <https://objectbox.io>
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

package io.objectbox.meshsync.android;

import android.content.Context;

import io.objectbox.BoxStore;
import io.objectbox.android.internal.meshsync.NearbyMeshNetwork;
import io.objectbox.annotation.apihint.Experimental;
import io.objectbox.exception.FeatureNotAvailableException;
import io.objectbox.sync.InternalSyncAccess;
import io.objectbox.sync.MeshConfig;
import io.objectbox.sync.SyncBuilder;
import io.objectbox.sync.SyncClient;

/**
 * Peer-to-peer mesh sync for Android using
 * <a href="https://developers.google.com/nearby/connections/overview">Google Nearby
 * Connections</a>.
 * <p>
 * Use {@link #createConfig(Context, String)} to create a {@link MeshConfig} with the Android mesh
 * network attached, then pass it to {@link SyncBuilder#mesh(MeshConfig)}:
 *
 * <pre>
 * MeshConfig meshConfig = AndroidMeshSync.createConfig(context, "com.myapp.mesh");
 *
 * SyncClient syncClient = Sync.client(boxStore)
 *         .url("ws://server:9999")
 *         .credentials(credentials)
 *         .mesh(meshConfig)
 *         .buildAndStart();
 * </pre>
 * <p>
 * The mesh starts and stops together with the sync client; query the running mesh via
 * {@link SyncClient#getMesh()}.
 * <p>
 * Requirements:
 * <ul>
 *     <li>The app must use the Sync variant of the ObjectBox Android library (e.g. {@code objectbox-sync-android}),
 *     which includes the native mesh sync code.</li>
 *     <li>This library adds the permissions required by Nearby Connections to the app manifest. However, the app
 *     must request the dangerous (runtime) permissions, like location and the newer Bluetooth/Wi-Fi permissions,
 *     before starting the sync client.</li>
 * </ul>
 */
@Experimental
public final class AndroidMeshSync {

    /**
     * Creates a mesh sync configuration with an Android (Nearby Connections) mesh network
     * attached.
     * <p>
     * Configure optional settings on the returned {@link MeshConfig} (chainable setters), then pass
     * it to {@link SyncBuilder#mesh(MeshConfig)}. See the
     * {@link AndroidMeshSync class documentation} for an example.
     *
     * @param context Android context (the application context is used internally).
     * @param meshId The mesh network identifier; nodes with different IDs ignore each other. Also
     * used as the Nearby Connections service ID, so it should be unique to your application, for
     * example based on your application ID (like {@code "com.myapp.mesh"}).
     * @return a {@link MeshConfig} to optionally configure further and pass to
     * {@link SyncBuilder#mesh(MeshConfig)}.
     */
    public static MeshConfig createConfig(Context context, String meshId) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null.");
        }
        if (!BoxStore.isSyncAvailable()) {
            throw new FeatureNotAvailableException(
                    "This library does not include ObjectBox Sync, which mesh sync requires. " +
                            "Update your dependencies to use the Sync variant (like objectbox-sync-android). " +
                            "Please visit https://objectbox.io/sync/ for options.");
        }
        // Create the config first: it validates meshId, avoiding the creation of a native network for bad input.
        MeshConfig config = InternalSyncAccess.createMeshConfig(meshId);
        // Create the Nearby network (Java + paired native object) and register it with the config. The native
        // network is owned by the MeshSync once the sync client is created; see NearbyMeshNetwork.stop().
        NearbyMeshNetwork network = new NearbyMeshNetwork(context.getApplicationContext(), meshId);
        InternalSyncAccess.addNetworkInternalHandle(config, network.getNativeHandle());
        return config;
    }

    private AndroidMeshSync() {
    }

}
