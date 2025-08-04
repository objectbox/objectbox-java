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

import java.io.Closeable;

import io.objectbox.BoxStore;
import io.objectbox.sync.server.SyncServer;

/**
 * Combines the functionality of a Sync client and a Sync server.
 * <p>
 * It is typically used in local cluster setups, in which a "hybrid" functions as a client and cluster peer (server).
 * <p>
 * Call {@link #getStore()} to retrieve the store. To set sync listeners use the {@link SyncClient} that is available
 * from {@link #getClient()}.
 * <p>
 * This class implements the {@link Closeable} interface, ensuring that resources are cleaned up properly.
 */
public final class SyncHybrid implements Closeable {
    private BoxStore store;
    private final SyncClient client;
    private BoxStore storeServer;
    private final SyncServer server;

    SyncHybrid(BoxStore store, SyncClient client, BoxStore storeServer, SyncServer server) {
        this.store = store;
        this.client = client;
        this.storeServer = storeServer;
        this.server = server;
    }

    public BoxStore getStore() {
        return store;
    }

    /**
     * Returns the {@link SyncClient} of this hybrid, typically only to set Sync listeners.
     * <p>
     * Note: do not stop or close the client directly. Instead, use the {@link #stop()} and {@link #close()} methods of
     * this hybrid.
     */
    public SyncClient getClient() {
        return client;
    }

    /**
     * Returns the {@link SyncServer} of this hybrid.
     * <p>
     * Typically, the server should not be touched. Yet, it is still exposed for advanced use cases.
     * <p>
     * Note: do not stop or close the server directly. Instead, use the {@link #stop()} and {@link #close()} methods of
     * this hybrid.
     */
    public SyncServer getServer() {
        return server;
    }

    /**
     * Stops the client and server.
     */
    public void stop() {
        client.stop();
        server.stop();
    }

    /**
     * Closes and cleans up all resources used by this Sync hybrid.
     * <p>
     * It can no longer be used afterward, build a new one instead.
     * <p>
     * Does nothing if this has already been closed.
     */
    @Override
    public void close() {
        // Clear reference to boxStore but do not close it (same behavior as SyncClient and SyncServer)
        store = null;
        client.close();
        server.close();
        if (storeServer != null) {
            storeServer.close(); // The server store is "internal", so can safely close it
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
