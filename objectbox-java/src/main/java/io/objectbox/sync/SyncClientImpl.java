package io.objectbox.sync;

import javax.annotation.Nullable;

import io.objectbox.InternalAccess;

class SyncClientImpl implements SyncClient {

    private final String url;
    private final SyncCredentialsImpl credentials;
    private final long storeHandle;

    private long syncClientHandle;

    SyncClientImpl(SyncBuilder syncBuilder) {
        this.url = syncBuilder.url;
        this.credentials = (SyncCredentialsImpl) syncBuilder.credentials;
        this.storeHandle = InternalAccess.getHandle(syncBuilder.boxStore);
    }

    @Override
    public String url() {
        return url;
    }

    public void connect(ConnectCallback callback) {
        if (syncClientHandle != 0) {
            callback.onComplete(null);
            return;
        }

        try {
            syncClientHandle = nativeCreate(storeHandle, url, null);

            nativeStart(syncClientHandle);

            byte[] credentialsBytes = null;
            if (credentials.getToken() != null) {
                //noinspection CharsetObjectCanBeUsed only added in Android API level 19 (K)
                credentialsBytes = credentials.getToken().getBytes("UTF-8");
            }
            nativeLogin(syncClientHandle, credentials.getTypeId(), credentialsBytes);

            callback.onComplete(null);
        } catch (Exception e) {
            callback.onComplete(e);
        }
    }

    @Override
    public void disconnect() {
        if (syncClientHandle == 0) return;

        try {
            nativeDelete(syncClientHandle);
        } catch (Exception ignored) {
        }
        syncClientHandle = 0;
    }

    static native long nativeCreate(long storeHandle, String uri, @Nullable String certificatePath);

    static native void nativeDelete(long handle);

    static native void nativeStart(long handle);

    static native void nativeLogin(long handle, int credentialsType, @Nullable byte[] credentials);
}
