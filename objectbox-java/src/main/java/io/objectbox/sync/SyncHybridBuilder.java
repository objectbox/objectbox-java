/*
 * Copyright 2024 ObjectBox Ltd.
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

import io.objectbox.BoxStore;
import io.objectbox.BoxStoreBuilder;
import io.objectbox.InternalAccess;
import io.objectbox.annotation.apihint.Internal;
import io.objectbox.sync.server.SyncServer;
import io.objectbox.sync.server.SyncServerBuilder;

/**
 * Builder for a Sync client and server hybrid setup, a {@link SyncHybrid}.
 * <p>
 * To change the server/cluster configuration, call {@link #serverBuilder()}, and for the client configuration
 * {@link #clientBuilder()}.
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public final class SyncHybridBuilder {

    private final BoxStore boxStore;
    private final BoxStore boxStoreServer;
    private final SyncBuilder clientBuilder;
    private final SyncServerBuilder serverBuilder;

    /**
     * Internal API; use {@link Sync#hybrid(BoxStoreBuilder, String, SyncCredentials)} instead.
     */
    @Internal
    SyncHybridBuilder(BoxStoreBuilder storeBuilder, String url, SyncCredentials authenticatorCredentials) {
        BoxStoreBuilder storeBuilderServer = InternalAccess.clone(storeBuilder, "-server");
        boxStore = storeBuilder.build();
        boxStoreServer = storeBuilderServer.build();
        SyncCredentials clientCredentials = authenticatorCredentials.createClone();
        // Do not yet set URL, port may only be chosen once server is started, see buildAndStart()
        clientBuilder = new SyncBuilder(boxStore).credentials(clientCredentials);
        serverBuilder = new SyncServerBuilder(boxStoreServer, url, authenticatorCredentials);
    }

    /**
     * Returns the builder of the client of the hybrid for additional configuration.
     */
    public SyncBuilder clientBuilder() {
        return clientBuilder;
    }

    /**
     * Returns the builder of the server of the hybrid for additional configuration.
     */
    public SyncServerBuilder serverBuilder() {
        return serverBuilder;
    }

    /**
     * Builds, starts and returns the hybrid.
     * <p>
     * Ensures the correct order of starting the server and client.
     */
    @SuppressWarnings("resource") // User is responsible for closing
    public SyncHybrid buildAndStart() {
        // Build and start the server first to obtain its URL, the port may have been set to 0 and dynamically assigned
        SyncServer server = serverBuilder.buildAndStart();

        SyncClient client = clientBuilder
                .url(server.getUrl())
                .buildAndStart();

        return new SyncHybrid(boxStore, client, boxStoreServer, server);
    }

}
