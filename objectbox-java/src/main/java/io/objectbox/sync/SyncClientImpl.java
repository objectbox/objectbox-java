package io.objectbox.sync;

import java.io.UnsupportedEncodingException;

import javax.annotation.Nullable;

import io.objectbox.InternalAccess;

public class SyncClientImpl implements SyncClient {

    private final String url;
    @Nullable private final String certificatePath;
    private final SyncCredentialsImpl credentials;
    private final long storeHandle;
    private final boolean manualUpdateRequests;

    private long syncClientHandle;
    @Nullable private SyncClientListener listener;
    @Nullable private SyncChangesListener syncChangesListener;

    SyncClientImpl(SyncBuilder syncBuilder) {
        this.url = syncBuilder.url;
        this.certificatePath = syncBuilder.certificatePath;
        this.credentials = (SyncCredentialsImpl) syncBuilder.credentials;
        this.storeHandle = InternalAccess.getHandle(syncBuilder.boxStore);
        this.manualUpdateRequests = syncBuilder.manualUpdateRequests;
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

    @Override
    public void setSyncChangesListener(SyncChangesListener changesListener) {
        checkNotNull(changesListener, "Listener must not be null. Use removeSyncChangesListener to remove existing listener.");
        this.syncChangesListener = changesListener;
        if (syncClientHandle != 0) {
            nativeSetSyncChangesListener(syncClientHandle, changesListener);
        }
    }

    @Override
    public void removeSyncChangesListener() {
        this.syncChangesListener = null;
        if (syncClientHandle != 0) {
            nativeSetSyncChangesListener(syncClientHandle, null);
        }
    }

    public synchronized void connect(ConnectCallback callback) {
        if (syncClientHandle != 0) {
            callback.onComplete(null);
            return;
        }

        try {
            syncClientHandle = nativeCreate(storeHandle, url, certificatePath);

            // if listeners were set before connecting register them now
            if (syncChangesListener != null) {
                nativeSetSyncChangesListener(syncClientHandle, syncChangesListener);
            }
            if (listener != null) {
                nativeSetListener(syncClientHandle, listener);
            }

            nativeStart(syncClientHandle);

            byte[] credentialsBytes = null;
            if (credentials.getToken() != null) {
                credentialsBytes = getAsBytesUtf8(credentials.getToken());
            }
            nativeLogin(syncClientHandle, credentials.getTypeId(), credentialsBytes);

            if(!manualUpdateRequests) {
                requestUpdates();
            }

            callback.onComplete(null);
        } catch (Exception e) {
            callback.onComplete(e);
        }
    }

    /** {@inheritDoc} */
    public synchronized void requestUpdates() {
        if (syncClientHandle == 0) return;
        nativeRequestUpdates(syncClientHandle, true);
    }

    /** {@inheritDoc} */
    public synchronized void requestUpdatesOnce() {
        if (syncClientHandle == 0) return;
        nativeRequestUpdates(syncClientHandle, false);
    }

    /** {@inheritDoc} */
    public synchronized void cancelUpdates() {
        if (syncClientHandle == 0) return;
        nativeCancelUpdates(syncClientHandle);
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

    static native void nativeSetSyncChangesListener(long handle, @Nullable SyncChangesListener advancedListener);

    /**
     * Request sync updates. Set {@code subscribeForPushes} to automatically receive updates for future changes.
     */
    static native void nativeRequestUpdates(long handle, boolean subscribeForPushes);

    /** (Optional) Cancel sync updates. */
    static native void nativeCancelUpdates(long handle);

}
