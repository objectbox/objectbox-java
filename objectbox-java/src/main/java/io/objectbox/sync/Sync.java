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
import io.objectbox.sync.server.SyncServer;
import io.objectbox.sync.server.SyncServerBuilder;

/**
 * <a href="https://objectbox.io/sync/">ObjectBox Sync</a> makes data available on other devices.
 * <p>
 * Use the static methods to build a Sync client or embedded server.
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
     * Starts building a {@link SyncClient}. Once done, complete with {@link SyncBuilder#build() build()}.
     *
     * @param boxStore The {@link BoxStore} the client should use.
     * @param url The URL of the Sync server on which the Sync protocol is exposed. This is typically a WebSockets URL
     * starting with {@code ws://} or {@code wss://} (for encrypted connections), for example
     * {@code ws://127.0.0.1:9999}.
     * @param credentials {@link SyncCredentials} to authenticate with the server.
     */
    public static SyncBuilder client(BoxStore boxStore, String url, SyncCredentials credentials) {
        return new SyncBuilder(boxStore, url, credentials);
    }

    /**
     * Starts building a {@link SyncClient}. Once done, complete with {@link SyncBuilder#build() build()}.
     *
     * @param boxStore The {@link BoxStore} the client should use.
     * @param url The URL of the Sync server on which the Sync protocol is exposed. This is typically a WebSockets URL
     * starting with {@code ws://} or {@code wss://} (for encrypted connections), for example
     * {@code ws://127.0.0.1:9999}.
     * @param multipleCredentials An array of {@link SyncCredentials} to be used to authenticate the user.
     */
    public static SyncBuilder client(BoxStore boxStore, String url, SyncCredentials[] multipleCredentials) {
        return new SyncBuilder(boxStore, url, multipleCredentials);
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
     * @param authenticatorCredentials An authentication method available to Sync clients and peers. Additional
     * authenticator credentials can be supplied using the returned builder. For the embedded server, currently only
     * {@link SyncCredentials#sharedSecret} and {@link SyncCredentials#none} are supported.
     */
    public static SyncServerBuilder server(BoxStore boxStore, String url, SyncCredentials authenticatorCredentials) {
        return new SyncServerBuilder(boxStore, url, authenticatorCredentials);
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
     * @param multipleAuthenticatorCredentials An authentication method available to Sync clients and peers. Additional
     * authenticator credentials can be supplied using the returned builder. For the embedded server, currently only
     * {@link SyncCredentials#sharedSecret} and {@link SyncCredentials#none} are supported.
     */
    public static SyncServerBuilder server(BoxStore boxStore, String url, SyncCredentials[] multipleAuthenticatorCredentials) {
        return new SyncServerBuilder(boxStore, url, multipleAuthenticatorCredentials);
    }

    /**
     * Starts building a {@link SyncHybrid}, a client/server hybrid typically used for embedded cluster setups.
     * <p>
     * Unlike {@link #client(BoxStore, String, SyncCredentials)} and {@link #server(BoxStore, String, SyncCredentials)},
     * the client Store is not built before. Instead, a Store builder must be passed. The client and server Store will
     * be built internally when calling this method.
     * <p>
     * To configure client and server use the methods on {@link SyncHybridBuilder}.
     *
     * @param storeBuilder The {@link BoxStoreBuilder} to use for building the client store.
     * @param url The URL of the Sync server on which the Sync protocol is exposed. This is typically a WebSockets URL
     * starting with {@code ws://} or {@code wss://} (for encrypted connections), for example
     * {@code ws://0.0.0.0:9999}.
     * @param authenticatorCredentials An authentication method available to Sync clients and peers. The client of the
     * hybrid is pre-configured with them. Additional credentials can be supplied using the client and server builder of
     * the returned builder. For the embedded server, currently only {@link SyncCredentials#sharedSecret} and
     * {@link SyncCredentials#none} are supported.
     * @return An instance of {@link SyncHybridBuilder}.
     */
    public static SyncHybridBuilder hybrid(BoxStoreBuilder storeBuilder, String url,
                                           SyncCredentials authenticatorCredentials) {
        return new SyncHybridBuilder(storeBuilder, url, authenticatorCredentials);
    }

    private Sync() {
    }
}
