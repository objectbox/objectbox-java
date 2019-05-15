package io.objectbox.sync;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import io.objectbox.InternalAccess;

public class SyncClientImpl implements SyncClient {

    private static final long LOGIN_TIMEOUT_SECONDS = 15;

    private final String url;
    private final InternalListener internalListener;
    private final boolean manualUpdateRequests;

    private volatile long handle;
    @Nullable private volatile SyncClientListener listener;

    private volatile long lastLoginCode;

    private volatile boolean started;

    SyncClientImpl(SyncBuilder builder) {
        this.url = builder.url;
        long storeHandle = InternalAccess.getHandle(builder.boxStore);
        this.manualUpdateRequests = builder.manualUpdateRequests;

        handle = nativeCreate(storeHandle, url, builder.certificatePath);
        if (handle == 0) {
            throw new RuntimeException("Handle is zero");
        }

        listener = builder.listener;

        internalListener = new InternalListener();
        nativeSetListener(handle, internalListener);

        if(builder.changesListener != null) {
            setSyncChangesListener(builder.changesListener);
        }

        setLoginCredentials(builder.credentials);

        if(!builder.manualStart) {
            start();
        }
    }

    @Override
    protected void finalize() throws Throwable {
        stop();
        super.finalize();
    }

    public synchronized void setLoginCredentials(SyncCredentials credentials) {
        if (handle == 0) return;
        byte[] credentialsBytes = SyncCredentialsToken.getTokenOrNull(credentials);
        nativeSetLoginInfo(handle, credentials.getTypeId(), credentialsBytes);
        credentials.clear();  // Clear immediately, not needed anymore
    }

    @Override
    public String url() {
        return url;
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
    public synchronized void setSyncChangesListener(SyncChangesListener changesListener) {
        checkNotNull(changesListener, "Listener must not be null. Use removeSyncChangesListener to remove existing listener.");
        if (handle != 0) {
            nativeSetSyncChangesListener(handle, changesListener);
        }
    }

    @Override
    public synchronized void removeSyncChangesListener() {
        if (handle != 0) {
            nativeSetSyncChangesListener(handle, null);
        }
    }

    public boolean awaitFirstLogin(long millisToWait) {
        if (!started) {
            start();
        }
        return internalListener.awaitFirstLogin(millisToWait);
    }

    public synchronized void start() {
        if (handle == 0) return;
        nativeStart(handle);
        started = true;
    }

    public boolean isStarted() {
        return started;
    }

    public synchronized void stop() {
        if (handle == 0) return;
        nativeStop(handle);
        started = false;
    }

    /** {@inheritDoc} */
    public synchronized void requestUpdates() {
        if (handle == 0) return;
        nativeRequestUpdates(handle, true);
    }

    /** {@inheritDoc} */
    public synchronized void requestUpdatesOnce() {
        if (handle == 0) return;
        nativeRequestUpdates(handle, false);
    }

    /** {@inheritDoc} */
    public synchronized void cancelUpdates() {
        if (handle == 0) return;
        nativeCancelUpdates(handle);
    }

    @Override
    public void close() {
        long handleToDelete;
        synchronized(this) {
            handleToDelete = this.handle;
            handle = 0;
        }

        if(handleToDelete != 0) {
            nativeDelete(handleToDelete);
        }
    }

    private void checkNotNull(Object object, String message) {
        if (object == null) {
            throw new IllegalArgumentException(message);
        }
    }

    public long getLastLoginCode() {
        return lastLoginCode;
    }

    public boolean isLoggedIn() {
        return lastLoginCode == 20;
    }

    private static native long nativeCreate(long storeHandle, String uri, @Nullable String certificatePath);

    private native void nativeDelete(long handle);

    private native void nativeStart(long handle);

    private native void nativeStop(long handle);

    private native void nativeSetLoginInfo(long handle, long credentialsType, @Nullable byte[] credentials);

    private native void nativeSetListener(long handle, @Nullable SyncClientListener listener);

    private native void nativeSetSyncChangesListener(long handle, @Nullable SyncChangesListener advancedListener);

    /**
     * Request sync updates. Set {@code subscribeForPushes} to automatically receive updates for future changes.
     */
    private native void nativeRequestUpdates(long handle, boolean subscribeForPushes);

    /** (Optional) Cancel sync updates. */
    private native void nativeCancelUpdates(long handle);

    private class InternalListener implements SyncClientListener {
        private final CountDownLatch firstLoginLatch = new CountDownLatch(1);

        @Override
        public void onLogin() {
            lastLoginCode = 20;
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

        boolean awaitFirstLogin(long millisToWait) {
            try {
                return firstLoginLatch.await(millisToWait, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                return false;
            }
        }
    }
}
