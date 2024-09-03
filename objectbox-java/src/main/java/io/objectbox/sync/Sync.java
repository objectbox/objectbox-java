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

import io.objectbox.BoxStore;
import io.objectbox.BoxStoreBuilder;
import io.objectbox.sync.server.SyncHybridBuilder;
import io.objectbox.sync.server.SyncServer;
import io.objectbox.sync.server.SyncServerBuilder;

/**
 * <a href="https://objectbox.io/sync/">ObjectBox Sync</a> makes data available on other devices.
 * Start building a sync client using Sync.{@link #client(BoxStore, String, SyncCredentials)}
 * or an embedded server using Sync.{@link #server(BoxStore, String, SyncCredentials)}.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public final class Sync {

    /**
     * Returns true if the included native (JNI) ObjectBox library supports Sync.
     */
    public static boolean isAvailable() {
        return BoxStore.isSyncAvailable();
    }

    /**
     * Returns true if the included native (JNI) ObjectBox library supports Sync server.
     */
    public static boolean isServerAvailable() {
        return BoxStore.isSyncServerAvailable();
    }

    /**
     * Returns true if the included native (JNI) ObjectBox library supports Sync hybrids (server & client).
     */
    public static boolean isHybridAvailable() {
        return isAvailable() && isServerAvailable();
    }

    /**
     * Start building a sync client. Requires the BoxStore that should be synced with the server,
     * the URL and port of the server to connect to and credentials to authenticate against the server.
     */
    public static SyncBuilder client(BoxStore boxStore, String url, SyncCredentials credentials) {
        return new SyncBuilder(boxStore, url, credentials);
    }

    /**
     * Starts building a {@link SyncServer}. Once done, complete with {@link SyncServerBuilder#build() build()}.
     * <p>
     * Note: when also using Admin, make sure it is started before the server.
     *
     * @param boxStore The {@link BoxStore} the server should use.
     * @param url The URL of the Sync server on which the Sync protocol is exposed. This is typically a WebSockets URL
     * starting with {@code ws://} or {@code wss://} (for encrypted connections), for example
     * {@code ws://0.0.0.0:9999}.
     * @param authenticatorCredentials A list of enabled authentication methods available to Sync clients. Additional
     * authenticator credentials can be supplied using the builder. For the embedded server, currently only
     * {@link SyncCredentials#sharedSecret} and {@link SyncCredentials#none} are supported.
     */
    public static SyncServerBuilder server(BoxStore boxStore, String url, SyncCredentials authenticatorCredentials) {
        return new SyncServerBuilder(boxStore, url, authenticatorCredentials);
    }

    /**
     * Starts building a {@link SyncHybridBuilder}, a client/server hybrid typically used for embedded cluster setups.
     * <p/>
     * Unlike {@link #client(BoxStore, String, SyncCredentials)} and {@link #server(BoxStore, String, SyncCredentials)},
     * you cannot pass in an already built store. Instead, you must pass in the store builder.
     * The store will be created internally when calling this method.
     * <p/>
     * As this is a hybrid, you can configure client and server aspects using the {@link SyncHybridBuilder}.
     *
     * @param storeBuilder the BoxStoreBuilder to use for building the main store.
     * @param url The URL of the Sync server on which the Sync protocol is exposed. This is typically a WebSockets URL
     * starting with {@code ws://} or {@code wss://} (for encrypted connections), for example
     * {@code ws://0.0.0.0:9999}.
     * @param authenticatorCredentials A list of enabled authentication methods available to Sync clients. Additional
     * authenticator credentials can be supplied using the builder. For the embedded server, currently only
     * {@link SyncCredentials#sharedSecret} and {@link SyncCredentials#none} are supported.
     * @return an instance of SyncHybridBuilder.
     */
    public static SyncHybridBuilder hybrid(BoxStoreBuilder storeBuilder, String url,
                                           SyncCredentials authenticatorCredentials) {
        return new SyncHybridBuilder(storeBuilder, url, authenticatorCredentials);
    }

    private Sync() {
    }
}
