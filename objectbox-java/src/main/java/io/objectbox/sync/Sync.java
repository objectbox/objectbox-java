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
     * Start building a sync client. Requires the BoxStore that should be synced with the server,
     * the URL and port of the server to connect to and credentials to authenticate against the server.
     */
    public static SyncBuilder client(BoxStore boxStore, String url, SyncCredentials credentials) {
        return new SyncBuilder(boxStore, url, credentials);
    }

    /**
     * Start building a sync server. Requires the BoxStore the server should use,
     * the URL and port the server should bind to and authenticator credentials to authenticate clients.
     * Additional authenticator credentials can be supplied using the builder.
     * <p>
     * For the embedded server, currently only {@link SyncCredentials#sharedSecret} and {@link SyncCredentials#none}
     * are supported.
     */
    public static SyncServerBuilder server(BoxStore boxStore, String url, SyncCredentials authenticatorCredentials) {
        return new SyncServerBuilder(boxStore, url, authenticatorCredentials);
    }

    private Sync() {
    }
}
