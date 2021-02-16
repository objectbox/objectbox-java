package io.objectbox.sync;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import io.objectbox.BoxStore;
import io.objectbox.InternalAccess;
import io.objectbox.annotation.apihint.Experimental;
import io.objectbox.annotation.apihint.Internal;
import io.objectbox.sync.SyncBuilder.RequestUpdatesMode;
import io.objectbox.sync.listener.SyncChangeListener;
import io.objectbox.sync.listener.SyncCompletedListener;
import io.objectbox.sync.listener.SyncConnectionListener;
import io.objectbox.sync.listener.SyncListener;
import io.objectbox.sync.listener.SyncLoginListener;

/**
 * Internal sync client implementation. Use {@link SyncClient} to access functionality,
 * this class may change without notice.
 */
@Internal
public class SyncClientImpl implements SyncClient {

    @Nullable
    private BoxStore boxStore;
    private final String serverUrl;
    private final InternalSyncClientListener internalListener;
    @Nullable
    private final ConnectivityMonitor connectivityMonitor;

    private volatile long handle;
    @Nullable
    private volatile SyncLoginListener loginListener;
    @Nullable
    private volatile SyncCompletedListener completedListener;
    @Nullable
    private volatile SyncConnectionListener connectionListener;
    private volatile long lastLoginCode;
    private volatile boolean started;

    SyncClientImpl(SyncBuilder builder) {
        this.boxStore = builder.boxStore;
        this.serverUrl = builder.url;
        this.connectivityMonitor = builder.platform.getConnectivityMonitor();

        long boxStoreHandle = InternalAccess.getHandle(builder.boxStore);
        this.handle = nativeCreate(boxStoreHandle, serverUrl, builder.trustedCertPaths);
        if (handle == 0) {
            throw new RuntimeException("Failed to create sync client: handle is zero.");
        }

        // Only change setting if not default (automatic sync updates and push subscription enabled).
        if (builder.requestUpdatesMode != RequestUpdatesMode.AUTO) {
            boolean autoRequestUpdates = builder.requestUpdatesMode != RequestUpdatesMode.MANUAL;
            nativeSetRequestUpdatesMode(handle, autoRequestUpdates, false);
        }
        // Only change setting if not default (uncommitted acks are off).
        if (builder.uncommittedAcks) {
            nativeSetUncommittedAcks(handle, true);
        }

        if (builder.listener != null) {
            setSyncListener(builder.listener);
        } else {
            this.loginListener = builder.loginListener;
            this.completedListener = builder.completedListener;
            if (builder.changeListener != null) {
                setSyncChangeListener(builder.changeListener);
            }
            this.connectionListener = builder.connectionListener;
        }

        this.internalListener = new InternalSyncClientListener();
        nativeSetListener(handle, internalListener);

        setLoginCredentials(builder.credentials);

        // If created successfully, let store keep a reference so the caller does not have to.
        InternalAccess.setSyncClient(builder.boxStore, this);
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

    /**
     * Gets the current state of this sync client. Throws if {@link #close()} was called.
     */
    public SyncState getSyncState() {
        return SyncState.fromId(nativeGetState(handle));
    }

    @Override
    public void setSyncLoginListener(@Nullable SyncLoginListener listener) {
        this.loginListener = listener;
    }

    @Override
    public void setSyncCompletedListener(@Nullable SyncCompletedListener listener) {
        this.completedListener = listener;
    }

    @Override
    public void setSyncChangeListener(@Nullable SyncChangeListener changesListener) {
        nativeSetSyncChangesListener(handle, changesListener);
    }

    @Override
    public void setSyncConnectionListener(@Nullable SyncConnectionListener listener) {
        this.connectionListener = listener;
    }

    @Override
    public void setSyncListener(@Nullable SyncListener listener) {
        this.loginListener = listener;
        this.completedListener = listener;
        this.connectionListener = listener;
        setSyncChangeListener(listener);
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
        if (connectivityMonitor != null) {
            connectivityMonitor.setObserver(this);
        }
    }

    @Override
    public boolean isStarted() {
        return started;
    }

    @Override
    public synchronized void stop() {
        if (connectivityMonitor != null) {
            connectivityMonitor.removeObserver();
        }

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
            if (connectivityMonitor != null) {
                connectivityMonitor.removeObserver();
            }

            // Remove instance reference from store, release store reference.
            BoxStore boxStore = this.boxStore;
            if (boxStore != null) {
                SyncClient syncClient = boxStore.getSyncClient();
                if (syncClient == this) {
                    InternalAccess.setSyncClient(boxStore, null);
                }
                this.boxStore = null;
            }

            handleToDelete = this.handle;
            handle = 0;
        }

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

    /**
     * Temporary only, try not to use it.
     */
    @Override
    @Experimental
    public boolean requestFullSync() {
        return nativeRequestFullSync(handle, false);
    }

    /**
     * Temporary only, try not to use it.
     */
    @Experimental
    public boolean requestFullSyncAndUpdates() {
        return nativeRequestFullSync(handle, true);
    }

    @Override
    public boolean requestUpdates() {
        return nativeRequestUpdates(handle, true);
    }

    @Override
    public boolean requestUpdatesOnce() {
        return nativeRequestUpdates(handle, false);
    }

    @Override
    public boolean cancelUpdates() {
        return nativeCancelUpdates(handle);
    }

    @Override
    public void notifyConnectionAvailable() {
        nativeTriggerReconnect(handle);
    }

    /**
     * Creates a native sync client for the given store handle ready to connect to the server at the given URI.
     * Uses certificate authorities trusted by the host if no trusted certificate paths are passed.
     */
    private static native long nativeCreate(long storeHandle, String uri, @Nullable String[] certificateDirsOrPaths);

    private native void nativeDelete(long handle);

    private native void nativeStart(long handle);

    private native void nativeStop(long handle);

    private native void nativeSetLoginInfo(long handle, long credentialsType, @Nullable byte[] credentials);

    private native void nativeSetListener(long handle, @Nullable InternalSyncClientListener listener);

    private native void nativeSetSyncChangesListener(long handle, @Nullable SyncChangeListener advancedListener);

    /** @param subscribeForPushes Pass true to automatically receive updates for future changes. */
    private native void nativeSetRequestUpdatesMode(long handle, boolean autoRequestUpdates, boolean subscribeForPushes);

    /**
     * @param uncommittedAcks Default is false.
     */
    private native void nativeSetUncommittedAcks(long handle, boolean uncommittedAcks);

    /**
     * Returns the current {@link SyncState} value.
     */
    private native int nativeGetState(long handle);

    /** @param subscribeForPushes Pass true to automatically receive updates for future changes. */
    private native boolean nativeRequestUpdates(long handle, boolean subscribeForPushes);

    /** @param subscribeForPushes Pass true to automatically receive updates for future changes. */
    private native boolean nativeRequestFullSync(long handle, boolean subscribeForPushes);

    /** (Optional) Pause sync updates. */
    private native boolean nativeCancelUpdates(long handle);

    /**
     *  Hints to the native client that an active network connection is available.
     *  Returns true if the native client was disconnected (and will try to re-connect).
     */
    private native boolean nativeTriggerReconnect(long handle);

    /**
     * Methods on this class must match those expected by JNI implementation.
     */
    @SuppressWarnings("unused") // Methods called from native code.
    private class InternalSyncClientListener {
        private final CountDownLatch firstLoginLatch = new CountDownLatch(1);

        public void onLogin() {
            lastLoginCode = SyncLoginCodes.OK;
            firstLoginLatch.countDown();

            SyncLoginListener listenerToFire = loginListener;
            if (listenerToFire != null) {
                listenerToFire.onLoggedIn();
            }
        }

        public void onLoginFailure(long errorCode) {
            lastLoginCode = errorCode;
            firstLoginLatch.countDown();

            SyncLoginListener listenerToFire = loginListener;
            if (listenerToFire != null) {
                listenerToFire.onLoginFailed(errorCode);
            }
        }

        public void onServerTimeUpdate(long serverTimeNanos) {
            // Not implemented, yet.
        }

        public void onSyncComplete() {
            SyncCompletedListener listenerToFire = completedListener;
            if (listenerToFire != null) {
                listenerToFire.onUpdatesCompleted();
            }
        }

        public void onDisconnect() {
            SyncConnectionListener listenerToFire = connectionListener;
            if (listenerToFire != null) {
                listenerToFire.onDisconnected();
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
