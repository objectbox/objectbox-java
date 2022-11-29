package io.objectbox.sync.server;

import javax.annotation.Nullable;

import io.objectbox.InternalAccess;
import io.objectbox.annotation.apihint.Internal;
import io.objectbox.sync.SyncCredentials;
import io.objectbox.sync.SyncCredentialsToken;
import io.objectbox.sync.listener.SyncChangeListener;

/**
 * Internal sync server implementation. Use {@link SyncServer} to access functionality,
 * this class may change without notice.
 */
@Internal
public class SyncServerImpl implements SyncServer {

    private final String url;
    private volatile long handle;

    @Nullable
    private volatile SyncChangeListener syncChangeListener;

    SyncServerImpl(SyncServerBuilder builder) {
        this.url = builder.url;

        long storeHandle = InternalAccess.getHandle(builder.boxStore);
        long handle = nativeCreate(storeHandle, url, builder.certificatePath);
        if (handle == 0) {
            throw new RuntimeException("Failed to create sync server: handle is zero.");
        }
        this.handle = handle;

        for (SyncCredentials credentials : builder.credentials) {
            SyncCredentialsToken credentialsInternal = (SyncCredentialsToken) credentials;
            nativeSetAuthenticator(handle, credentialsInternal.getTypeId(), credentialsInternal.getTokenBytes());
            credentialsInternal.clear(); // Clear immediately, not needed anymore.
        }

        for (PeerInfo peer : builder.peers) {
            SyncCredentialsToken credentialsInternal = (SyncCredentialsToken) peer.credentials;
            nativeAddPeer(handle, peer.url, credentialsInternal.getTypeId(), credentialsInternal.getTokenBytes());
        }

        if (builder.changeListener != null) {
            setSyncChangeListener(builder.changeListener);
        }
    }

    private long getHandle() {
        long handle = this.handle;
        if (handle == 0) {
            throw new IllegalStateException("SyncServer already closed");
        }
        return handle;
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public int getPort() {
        return nativeGetPort(getHandle());
    }

    @Override
    public boolean isRunning() {
        return nativeIsRunning(getHandle());
    }

    @Override
    public String getStatsString() {
        return nativeGetStatsString(getHandle());
    }

    @Override
    public void setSyncChangeListener(@Nullable SyncChangeListener changesListener) {
        this.syncChangeListener = changesListener;
        nativeSetSyncChangesListener(getHandle(), changesListener);
    }

    @Override
    public void start() {
        nativeStart(getHandle());
    }

    @Override
    public void stop() {
        nativeStop(getHandle());
    }

    @Override
    public void close() {
        long handleToDelete = handle;
        handle = 0;
        if (handleToDelete != 0) {
            nativeDelete(handleToDelete);
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

    private static native long nativeCreate(long storeHandle, String uri, @Nullable String certificatePath);

    private native void nativeDelete(long handle);

    private native void nativeStart(long handle);

    private native void nativeStop(long handle);

    private native boolean nativeIsRunning(long handle);

    private native int nativeGetPort(long handle);

    private native void nativeSetAuthenticator(long handle, long credentialsType, @Nullable byte[] credentials);

    private native void nativeAddPeer(long handle, String uri, long credentialsType, @Nullable byte[] credentials);

    private native String nativeGetStatsString(long handle);

    private native void nativeSetSyncChangesListener(long handle, @Nullable SyncChangeListener changesListener);

}
