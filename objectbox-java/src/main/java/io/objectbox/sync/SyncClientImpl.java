package io.objectbox.sync;

import java.io.UnsupportedEncodingException;

import javax.annotation.Nullable;

import io.objectbox.InternalAccess;

class SyncClientImpl implements SyncClient {

    private final String url;
    @Nullable private final String certificatePath;
    private final SyncCredentialsImpl credentials;
    private final long storeHandle;

    private long syncClientHandle;
    @Nullable private SyncClientListener listener;

    SyncClientImpl(SyncBuilder syncBuilder) {
        this.url = syncBuilder.url;
        this.certificatePath = syncBuilder.certificatePath;
        this.credentials = (SyncCredentialsImpl) syncBuilder.credentials;
        this.storeHandle = InternalAccess.getHandle(syncBuilder.boxStore);
    }

    @Override
    public String url() {
        return url;
    }

    @Override
    public synchronized void setSyncListener(SyncClientListener listener) {
        checkNotNull(listener, "Listener must not be null. Use removeSyncListener to remove existing listener.");
        this.listener = listener;
        if (syncClientHandle != 0) {
            nativeSetListener(syncClientHandle, listener);
        }
    }

    @Override
    public synchronized void removeSyncListener() {
        this.listener = null;
        if (syncClientHandle != 0) {
            nativeSetListener(syncClientHandle, null);
        }
    }

    public synchronized void connect(ConnectCallback callback) {
        if (syncClientHandle != 0) {
            callback.onComplete(null);
            return;
        }

        try {
            // JNI does not accept null value for certificatePath
            String safeCertificatePath = certificatePath != null ? certificatePath : "";
            syncClientHandle = nativeCreate(storeHandle, url, safeCertificatePath);

            nativeStart(syncClientHandle);

            // JNI does not accept null value for credentialsBytes
            byte[] credentialsBytes = new byte[]{};
            if (credentials.getToken() != null) {
                credentialsBytes = getAsBytesUtf8(credentials.getToken());
            }
            nativeLogin(syncClientHandle, credentials.getTypeId(), credentialsBytes);

            // if listener was set before connecting register it now
            if (listener != null) {
                nativeSetListener(syncClientHandle, listener);
            }

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

    private void checkNotNull(Object object, String message) {
        if (object == null) {
            throw new IllegalArgumentException(message);
        }
    }

    static native long nativeCreate(long storeHandle, String uri, @Nullable String certificatePath);

    static native void nativeDelete(long handle);

    static native void nativeStart(long handle);

    static native void nativeLogin(long handle, int credentialsType, @Nullable byte[] credentials);

    static native void nativeSetListener(long handle, @Nullable SyncClientListener listener);

}
