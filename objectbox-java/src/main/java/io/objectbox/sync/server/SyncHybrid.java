/*
 * Copyright 2024 ObjectBox Ltd. All rights reserved.
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

package io.objectbox.sync.server;

import java.io.Closeable;

import io.objectbox.BoxStore;
import io.objectbox.sync.SyncClient;

/**
 * The SyncHybrid combines the functionality of a Sync Client and a Sync Server.
 * It is typically used in local cluster setups, in which a "hybrid" functions as a client & cluster peer (server).
 * <p/>
 * Call {@link #getStore()} to retrieve the store.
 * To set sync listeners use the {@link SyncClient} that is available from {@link #getClient()}.
 * <p/>
 * This class implements the Closeable interface, ensuring that resources are cleaned up properly.
 */
public final class SyncHybrid implements Closeable {
    private BoxStore store;
    private final SyncClient client;
    private BoxStore storeServer;
    private final SyncServer server;

    public SyncHybrid(BoxStore store, SyncClient client, BoxStore storeServer, SyncServer server) {
        this.store = store;
        this.client = client;
        this.storeServer = storeServer;
        this.server = server;
    }

    public BoxStore getStore() {
        return store;
    }

    /**
     * Typically only used to set sync listeners.
     * <p/>
     * Note: you should not directly call start(), stop(), close() on the {@link SyncClient} directly.
     * Instead, call {@link #stop()} or {@link #close()} on this instance (it is already started during creation).
     */
    public SyncClient getClient() {
        return client;
    }

    /**
     * Typically, you won't need access to the SyncServer.
     * It is still exposed for advanced use cases if you know what you are doing.
     * <p/>
     * Note: you should not directly call start(), stop(), close() on the {@link SyncServer} directly.
     * Instead, call {@link #stop()} or {@link #close()} on this instance (it is already started during creation).
     */
    public SyncServer getServer() {
        return server;
    }

    public void stop() {
        client.stop();
        server.stop();
    }

    @Override
    public void close() {
        // Clear reference to boxStore but do not close it (same behavior as SyncClient and SyncServer)
        store = null;
        client.close();
        server.close();
        if (storeServer != null) {
            storeServer.close();  // The server store is "internal", so we can close it
            storeServer = null;
        }
    }

    /**
     * Users of this class should explicitly call {@link #close()} instead to avoid expensive finalization.
     */
    @SuppressWarnings("deprecation") // finalize()
    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }
}
