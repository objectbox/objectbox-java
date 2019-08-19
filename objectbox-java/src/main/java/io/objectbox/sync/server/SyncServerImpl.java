package io.objectbox.sync.server;

import io.objectbox.InternalAccess;
import io.objectbox.sync.SyncChangesListener;
import io.objectbox.sync.SyncCredentials;
import io.objectbox.sync.SyncCredentialsToken;

import javax.annotation.Nullable;

/**
 * Internal sync server implementation. Use {@link SyncServer} to access functionality,
 * this class may change without notice.
 */
public class SyncServerImpl implements SyncServer {

    private final String url;
    private volatile long handle;

    @Nullable
    private volatile SyncChangesListener syncChangesListener;

    SyncServerImpl(SyncServerBuilder builder) {
        this.url = builder.url;

        long storeHandle = InternalAccess.getHandle(builder.boxStore);
        handle = nativeCreate(storeHandle, url, builder.certificatePath);
        if (handle == 0) {
            throw new RuntimeException("Failed to create sync server: handle is zero.");
        }

        for (SyncCredentials credentials : builder.credentials) {
            SyncCredentialsToken credentialsInternal = (SyncCredentialsToken) credentials;
            nativeSetAuthenticator(handle, credentialsInternal.getTypeId(), credentialsInternal.getTokenBytes());
            credentialsInternal.clear(); // Clear immediately, not needed anymore.
        }

        for (PeerInfo peer : builder.peers) {
            SyncCredentialsToken credentialsInternal = (SyncCredentialsToken) peer.credentials;
            nativeAddPeer(handle, peer.url, credentialsInternal.getTypeId(), credentialsInternal.getTokenBytes());
        }

        if (builder.changesListener != null) {
            setSyncChangesListener(builder.changesListener);
        }

        if (!builder.manualStart) {
            start();
        }
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public int getPort() {
        return nativeGetPort(handle);
    }

    @Override
    public boolean isRunning() {
        return nativeIsRunning(handle);
    }

    @Override
    public String getStatsString() {
        return nativeGetStatsString(handle);
    }

    @Override
    public void setSyncChangesListener(SyncChangesListener changesListener) {
        checkNotNull(changesListener, "Listener must not be null. Use removeSyncChangesListener to remove existing listener.");
        this.syncChangesListener = changesListener;
        nativeSetSyncChangesListener(handle, changesListener);
    }

    @Override
    public void removeSyncChangesListener() {
        this.syncChangesListener = null;
        nativeSetSyncChangesListener(handle, null);
    }

    @Override
    public void start() {
        nativeStart(handle);
    }

    @Override
    public void stop() {
        nativeStop(handle);
    }

    @Override
    public void close() {
        long handleToDelete = handle;
        handle = 0;
        if (handleToDelete != 0) {
            nativeDelete(handleToDelete);
        }
    }

    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

    private void checkNotNull(Object object, String message) {
        if (object == null) {
            throw new IllegalArgumentException(message);
        }
    }

    private static native long nativeCreate(long storeHandle, String uri, @Nullable String certificatePath);

    private native void nativeDelete(long handle);

    private native void nativeStart(long handle);

    private native void nativeStop(long handle);

    private native boolean nativeIsRunning(long handle);

    private native int nativeGetPort(long handle);

    private native void nativeSetAuthenticator(long handle, long credentialsType, @Nullable byte[] credentials);

    private native void nativeAddPeer(long handle, String uri, long credentialsType, @Nullable byte[] credentials);

    private native String nativeGetStatsString(long handle);

    private native void nativeSetSyncChangesListener(long handle, @Nullable SyncChangesListener changesListener);

}
