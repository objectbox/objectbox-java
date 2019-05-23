package io.objectbox.sync.server;

import io.objectbox.BoxStore;
import io.objectbox.sync.SyncChangesListener;
import io.objectbox.sync.SyncCredentials;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/** Creates a {@link SyncServer} and allows to set additional configuration. */
@SuppressWarnings({"unused", "UnusedReturnValue", "WeakerAccess"})
public class SyncServerBuilder {

    final BoxStore boxStore;
    final String url;
    @Nullable String certificatePath;
    SyncChangesListener changesListener;
    boolean manualStart;

    final List<SyncCredentials> credentials = new ArrayList<>();
    final List<PeerInfo> peers = new ArrayList<>();

    public SyncServerBuilder(BoxStore boxStore, String url, SyncCredentials authenticatorCredentials) {
        checkNotNull(boxStore, "BoxStore is required.");
        checkNotNull(url, "Sync server URL is required.");
        checkNotNull(authenticatorCredentials, "Authenticator credentials is required.");
        if (!BoxStore.isSyncServerAvailable()) {
            throw new IllegalStateException(
                    "This ObjectBox library (JNI) does not include sync server. Please update your dependencies.");
        }
        this.boxStore = boxStore;
        this.url = url;
        authenticatorCredentials(authenticatorCredentials);
    }

    public SyncServerBuilder certificatePath(String certificatePath) {
        this.certificatePath = certificatePath;
        return this;
    }

    /** Provides additional authenticator credentials */
    public SyncServerBuilder authenticatorCredentials(SyncCredentials authenticatorCredentials) {
        checkNotNull(authenticatorCredentials, "Authenticator credentials is required.");
        credentials.add(authenticatorCredentials);
        return this;
    }

    /**
     * By default, sync automatically starts; with this, you can override this behavior.
     * @see SyncServer#start()
     */
    public SyncServerBuilder manualStart() {
        manualStart = true;
        return this;
    }

    /**
     * Sets the synchronization listener.
     * @see SyncServer#setSyncChangesListener(SyncChangesListener)
     */
    public SyncServerBuilder changesListener(SyncChangesListener changesListener) {
        this.changesListener = changesListener;
        return this;
    }

    /** Adds a server peer, to which we connect to as a client using {@link SyncCredentials#none()}. */
    public SyncServerBuilder peer(String url) {
        return peer(url, SyncCredentials.none());
    }

    /** Adds a server peer, to which we connect to as a client using the given credentials. */
    public SyncServerBuilder peer(String url, SyncCredentials credentials) {
        peers.add(new PeerInfo(url, credentials));
        return this;
    }

    /** Note: this clears all previously set authenticator credentials. */
    public SyncServer build() {
        SyncServerImpl syncServer = new SyncServerImpl(this);
        credentials.clear();  // Those are cleared anyway by now
        return syncServer;
    }

    private void checkNotNull(Object object, String message) {
        if (object == null) {
            throw new IllegalArgumentException(message);
        }
    }

}
