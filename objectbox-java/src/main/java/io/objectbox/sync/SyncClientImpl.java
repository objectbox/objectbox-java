package io.objectbox.sync;

import io.objectbox.InternalAccess;
import io.objectbox.annotation.apihint.Experimental;
import io.objectbox.annotation.apihint.Internal;

import javax.annotation.Nullable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Internal sync client implementation. Use {@link SyncClient} to access functionality,
 * this class may change without notice.
 */
@Internal
public class SyncClientImpl implements SyncClient {

    private final String serverUrl;
    private final InternalListener internalListener;
    private final boolean manualUpdateRequests;

    private volatile long handle;
    @Nullable
    private volatile SyncClientListener listener;
    private volatile long lastLoginCode;
    private volatile boolean started;

    SyncClientImpl(SyncBuilder builder) {
        this.serverUrl = builder.url;
        this.manualUpdateRequests = builder.manualUpdateRequests;

        long boxStoreHandle = InternalAccess.getHandle(builder.boxStore);
        this.handle = nativeCreate(boxStoreHandle, serverUrl, builder.certificatePath);
        if (handle == 0) {
            throw new RuntimeException("Failed to create sync client: handle is zero.");
        }

        this.listener = builder.listener;

        this.internalListener = new InternalListener();
        nativeSetListener(handle, internalListener);

        if (builder.changesListener != null) {
            setSyncChangesListener(builder.changesListener);
        }

        setLoginCredentials(builder.credentials);

        if (!builder.manualStart) {
            start();
        }
    }

    @Override
    public String getServerUrl() {
        return serverUrl;
    }

    @Override
    public long getLastLoginCode() {
        return lastLoginCode;
    }

    @Override
    public boolean isLoggedIn() {
        return lastLoginCode == SyncLoginCodes.OK;
    }

    @Override
    public void setSyncListener(SyncClientListener listener) {
        checkNotNull(listener, "Listener must not be null. Use removeSyncListener to remove existing listener.");
        this.listener = listener;
    }

    @Override
    public void removeSyncListener() {
        this.listener = null;
    }

    @Override
    public void setSyncChangesListener(SyncChangesListener changesListener) {
        checkNotNull(changesListener, "Listener must not be null. Use removeSyncChangesListener to remove existing listener.");
        nativeSetSyncChangesListener(handle, changesListener);
    }

    @Override
    public void removeSyncChangesListener() {
        nativeSetSyncChangesListener(handle, null);
    }

    @Override
    public void setLoginCredentials(SyncCredentials credentials) {
        SyncCredentialsToken credentialsInternal = (SyncCredentialsToken) credentials;
        nativeSetLoginInfo(handle, credentialsInternal.getTypeId(), credentialsInternal.getTokenBytes());
        credentialsInternal.clear(); // Clear immediately, not needed anymore.
    }

    @Override
    public boolean awaitFirstLogin(long millisToWait) {
        if (!started) {
            start();
        }
        return internalListener.awaitFirstLogin(millisToWait);
    }

    @Override
    public synchronized void start() {
        nativeStart(handle);
        started = true;
    }

    @Override
    public boolean isStarted() {
        return started;
    }

    @Override
    public synchronized void stop() {
        long handleToStop = this.handle;
        if (handleToStop != 0) {
            nativeStop(handleToStop);
        }
        started = false;
    }

    @Override
    public void close() {
        long handleToDelete;
        synchronized (this) {
            handleToDelete = this.handle;
            handle = 0;
        }

        if (handleToDelete != 0) {
            nativeDelete(handleToDelete);
        }
    }

    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

    @Override
    public void requestFullSync() {
        nativeRequestFullSync(handle, false);
    }

    // TODO: broken?
    @Experimental
    public void requestFullSyncAndUpdates() {
        nativeRequestFullSync(handle, true);
    }

    @Override
    public void requestUpdates() {
        nativeRequestUpdates(handle, true);
    }

    @Override
    public void requestUpdatesOnce() {
        nativeRequestUpdates(handle, false);
    }

    @Override
    public void cancelUpdates() {
        nativeCancelUpdates(handle);
    }

    private void checkNotNull(Object object, String message) {
        //noinspection ConstantConditions Non-null annotation does not enforce, so check for null.
        if (object == null) {
            throw new IllegalArgumentException(message);
        }
    }

    private static native long nativeCreate(long storeHandle, String uri, @Nullable String certificatePath);

    private native void nativeDelete(long handle);

    private native void nativeStart(long handle);

    private native void nativeStop(long handle);

    private native void nativeSetLoginInfo(long handle, long credentialsType, @Nullable byte[] credentials);

    private native void nativeSetListener(long handle, @Nullable SyncClientListener listener);

    private native void nativeSetSyncChangesListener(long handle, @Nullable SyncChangesListener advancedListener);

    /** @param subscribeForPushes Pass true to automatically receive updates for future changes. */
    private native void nativeRequestUpdates(long handle, boolean subscribeForPushes);

    /** @param subscribeForPushes Pass true to automatically receive updates for future changes. */
    private native void nativeRequestFullSync(long handle, boolean subscribeForPushes);

    /** (Optional) Pause sync updates. */
    private native void nativeCancelUpdates(long handle);

    private class InternalListener implements SyncClientListener {
        private final CountDownLatch firstLoginLatch = new CountDownLatch(1);

        @Override
        public void onLogin() {
            lastLoginCode = SyncLoginCodes.OK;
            firstLoginLatch.countDown();
            if (!manualUpdateRequests) {
                requestUpdates();
            }
            SyncClientListener listenerToFire = listener;
            if (listenerToFire != null) {
                listenerToFire.onLogin();
            }
        }

        @Override
        public void onLoginFailure(long errorCode) {
            lastLoginCode = errorCode;
            firstLoginLatch.countDown();

            SyncClientListener listenerToFire = listener;
            if (listenerToFire != null) {
                listenerToFire.onLoginFailure(errorCode);
            }
        }

        @Override
        public void onSyncComplete() {
            SyncClientListener listenerToFire = listener;
            if (listenerToFire != null) {
                listenerToFire.onSyncComplete();
            }
        }

        @Override
        public void onDisconnect() {
            SyncClientListener listenerToFire = listener;
            if (listenerToFire != null) {
                listenerToFire.onDisconnect();
            }
        }

        boolean awaitFirstLogin(long millisToWait) {
            try {
                return firstLoginLatch.await(millisToWait, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                return false;
            }
        }
    }
}
