package io.objectbox.sync.server;

import io.objectbox.InternalAccess;
import io.objectbox.sync.SyncChangesListener;
import io.objectbox.sync.SyncCredentials;
import io.objectbox.sync.SyncCredentialsToken;

import javax.annotation.Nullable;
import java.util.List;

public class SyncServerImpl implements SyncServer {

    private final String url;
    private volatile long handle;

    @Nullable
    private volatile SyncChangesListener syncChangesListener;

    SyncServerImpl(SyncServerBuilder syncServerBuilder) {
        this.url = syncServerBuilder.url;
        List<SyncCredentials> credentialsList = syncServerBuilder.credentials;
        if (credentialsList.isEmpty()) {
            throw new IllegalStateException("You must provide at least one authenticator");
        }
        long storeHandle = InternalAccess.getHandle(syncServerBuilder.boxStore);
        handle = nativeCreate(storeHandle, url, syncServerBuilder.certificatePath);
        if (handle == 0) {
            throw new RuntimeException("Handle is zero");
        }
        for (SyncCredentials credentials : credentialsList) {
            byte[] credentialsBytes = SyncCredentialsToken.getTokenOrNull(credentials);
            nativeSetAuthenticator(handle, credentials.getTypeId(), credentialsBytes);
            credentials.clear();
        }
    }

    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

    @Override
    public String url() {
        return url;
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
    public String getStatsString() {
        return nativeGetStatsString(handle);
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

    private native void nativeSetAuthenticator(long handle, long credentialsType, @Nullable byte[] credentials);

    private native String nativeGetStatsString(long handle);

    private native void nativeSetSyncChangesListener(long handle, @Nullable SyncChangesListener changesListener);

}
