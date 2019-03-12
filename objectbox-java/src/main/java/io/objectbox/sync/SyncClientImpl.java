package io.objectbox.sync;

import java.io.UnsupportedEncodingException;

import javax.annotation.Nullable;

import io.objectbox.InternalAccess;

class SyncClientImpl implements SyncClient {

    private final String url;
    @Nullable private final String certificatePath;
    private final String objectBoxClientId;
    private final SyncCredentialsImpl credentials;
    private final long storeHandle;

    private long syncClientHandle;

    SyncClientImpl(SyncBuilder syncBuilder) {
        this.url = syncBuilder.url;
        this.certificatePath = syncBuilder.certificatePath;
        this.objectBoxClientId = syncBuilder.objectBoxClientId;
        this.credentials = (SyncCredentialsImpl) syncBuilder.credentials;
        this.storeHandle = InternalAccess.getHandle(syncBuilder.boxStore);
    }

    @Override
    public String url() {
        return url;
    }

    public synchronized void connect(ConnectCallback callback) {
        if (syncClientHandle != 0) {
            callback.onComplete(null);
            return;
        }

        try {
            syncClientHandle = nativeCreate(storeHandle, url, certificatePath);

            nativeStart(syncClientHandle);

            byte[] credentialsBytes = null;
            if (credentials.getToken() != null) {
                credentialsBytes = getAsBytesUtf8(credentials.getToken());
            }
            nativeLogin(syncClientHandle, getAsBytesUtf8(objectBoxClientId), credentials.getTypeId(), credentialsBytes);

            callback.onComplete(null);
        } catch (Exception e) {
            callback.onComplete(e);
        }
    }

    @Override
    public synchronized void disconnect() {
        if (syncClientHandle == 0) return;

        try {
            nativeDelete(syncClientHandle);
        } catch (Exception ignored) {
        }
        syncClientHandle = 0;
    }

    private byte[] getAsBytesUtf8(String text) throws UnsupportedEncodingException {
        //noinspection CharsetObjectCanBeUsed only added in Android API level 19 (K)
        return text.getBytes("UTF-8");
    }

    static native long nativeCreate(long storeHandle, String uri, @Nullable String certificatePath);

    static native void nativeDelete(long handle);

    static native void nativeStart(long handle);

    static native void nativeLogin(long handle, byte[] clientId, int credentialsType, @Nullable byte[] credentials);
}
