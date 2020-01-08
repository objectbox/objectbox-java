package io.objectbox.sync;

import io.objectbox.BoxStore;
import io.objectbox.annotation.apihint.Experimental;
import io.objectbox.sync.server.SyncServerBuilder;

/**
 * Start building a sync client using Sync.{@link #client(BoxStore, String, SyncCredentials)}
 * or a server using Sync.{@link #server(BoxStore, String, SyncCredentials)}.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
@Experimental
public final class Sync {

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
     */
    public static SyncServerBuilder server(BoxStore boxStore, String url, SyncCredentials authenticatorCredentials) {
        return new SyncServerBuilder(boxStore, url, authenticatorCredentials);
    }

    private Sync() {
    }
}
