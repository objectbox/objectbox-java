package io.objectbox.sync;

import io.objectbox.BoxStore;
import io.objectbox.InternalAccess;
import io.objectbox.annotation.apihint.Experimental;
import io.objectbox.annotation.apihint.Internal;
import io.objectbox.sync.SyncBuilder.RequestUpdatesMode;

import javax.annotation.Nullable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Internal sync client implementation. Use {@link SyncClient} to access functionality,
 * this class may change without notice.
 */
@Internal
public class SyncClientImpl implements SyncClient {

    @Nullable
    private BoxStore boxStore;
    private final String serverUrl;
    private final InternalListener internalListener;
    @Nullable
    private final ConnectivityMonitor connectivityMonitor;

    private volatile long handle;
    @Nullable
    private volatile SyncClientListener listener;
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

        this.listener = builder.listener;

        this.internalListener = new InternalListener();
        nativeSetListener(handle, internalListener);

        if (builder.changesListener != null) {
            setSyncChangesListener(builder.changesListener);
        }

        setLoginCredentials(builder.credentials);

        // If created successfully, let store keep a reference so the caller does not have to.
        InternalAccess.setSyncClient(builder.boxStore, this);

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

    /**
     * Gets the current state of this sync client. Throws if {@link #close()} was called.
     */
    public SyncClientState getSyncState() {
        return SyncClientState.fromId(nativeGetState(handle));
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

    @Override
    public void notifyConnectionAvailable() {
        nativeTriggerReconnect(handle);
    }

    private void checkNotNull(Object object, String message) {
        //noinspection ConstantConditions Non-null annotation does not enforce, so check for null.
        if (object == null) {
            throw new IllegalArgumentException(message);
        }
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

    private native void nativeSetListener(long handle, @Nullable SyncClientListener listener);

    private native void nativeSetSyncChangesListener(long handle, @Nullable SyncChangesListener advancedListener);

    /** @param subscribeForPushes Pass true to automatically receive updates for future changes. */
    private native void nativeSetRequestUpdatesMode(long handle, boolean autoRequestUpdates, boolean subscribeForPushes);

    /**
     * @param uncommittedAcks Default is false.
     */
    private native void nativeSetUncommittedAcks(long handle, boolean uncommittedAcks);

    /**
     * Returns the current {@link SyncClientState} value.
     */
    private native int nativeGetState(long handle);

    /** @param subscribeForPushes Pass true to automatically receive updates for future changes. */
    private native void nativeRequestUpdates(long handle, boolean subscribeForPushes);

    /** @param subscribeForPushes Pass true to automatically receive updates for future changes. */
    private native void nativeRequestFullSync(long handle, boolean subscribeForPushes);

    /** (Optional) Pause sync updates. */
    private native void nativeCancelUpdates(long handle);

    /** Hints to the native client that an active network connection is available. */
    private native void nativeTriggerReconnect(long handle);

    private class InternalListener implements SyncClientListener {
        private final CountDownLatch firstLoginLatch = new CountDownLatch(1);

        @Override
        public void onLogin() {
            lastLoginCode = SyncLoginCodes.OK;
            firstLoginLatch.countDown();

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
