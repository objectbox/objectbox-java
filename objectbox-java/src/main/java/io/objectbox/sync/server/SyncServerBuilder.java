package io.objectbox.sync.server;

import io.objectbox.BoxStore;
import io.objectbox.annotation.apihint.Experimental;
import io.objectbox.sync.SyncChangesListener;
import io.objectbox.sync.SyncCredentials;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Creates a {@link SyncServer} and allows to set additional configuration.
 */
@SuppressWarnings({"unused", "UnusedReturnValue", "WeakerAccess"})
@Experimental
public class SyncServerBuilder {

    final BoxStore boxStore;
    final String url;
    final List<SyncCredentials> credentials = new ArrayList<>();
    final List<PeerInfo> peers = new ArrayList<>();

    @Nullable String certificatePath;
    SyncChangesListener changesListener;
    boolean manualStart;

    public SyncServerBuilder(BoxStore boxStore, String url, SyncCredentials authenticatorCredentials) {
        checkNotNull(boxStore, "BoxStore is required.");
        checkNotNull(url, "Sync server URL is required.");
        checkNotNull(authenticatorCredentials, "Authenticator credentials are required.");
        if (!BoxStore.isSyncServerAvailable()) {
            throw new IllegalStateException(
                    "This ObjectBox library (JNI) does not include sync server. Check your dependencies.");
        }
        this.boxStore = boxStore;
        this.url = url;
        authenticatorCredentials(authenticatorCredentials);
    }

    public SyncServerBuilder certificatePath(String certificatePath) {
        this.certificatePath = certificatePath;
        return this;
    }

    /**
     * Adds additional authenticator credentials to authenticate clients with.
     */
    public SyncServerBuilder authenticatorCredentials(SyncCredentials authenticatorCredentials) {
        checkNotNull(authenticatorCredentials, "Authenticator credentials must not be null.");
        credentials.add(authenticatorCredentials);
        return this;
    }

    /**
     * Prevents the server from starting automatically.
     * It will need to be started manually later.
     *
     * @see SyncServer#start()
     */
    public SyncServerBuilder manualStart() {
        manualStart = true;
        return this;
    }

    /**
     * Sets a listener to observe fine granular changes happening during sync.
     * This listener can also be set (or removed) on the sync client directly.
     *
     * @see SyncServer#setSyncChangesListener(SyncChangesListener)
     */
    public SyncServerBuilder changesListener(SyncChangesListener changesListener) {
        this.changesListener = changesListener;
        return this;
    }

    /**
     * Adds a server peer, to which this server should connect to as a client using {@link SyncCredentials#none()}.
     */
    public SyncServerBuilder peer(String url) {
        return peer(url, SyncCredentials.none());
    }

    /**
     * Adds a server peer, to which this server should connect to as a client using the given credentials.
     */
    public SyncServerBuilder peer(String url, SyncCredentials credentials) {
        peers.add(new PeerInfo(url, credentials));
        return this;
    }

    /**
     * Note: this clears all previously set authenticator credentials.
     */
    public SyncServer build() {
        if (credentials.isEmpty()) {
            throw new IllegalStateException("At least one authenticator is required.");
        }
        return new SyncServerImpl(this);
    }

    private void checkNotNull(Object object, String message) {
        if (object == null) {
            throw new IllegalArgumentException(message);
        }
    }

}
