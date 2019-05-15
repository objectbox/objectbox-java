package io.objectbox.sync;

import java.rmi.ConnectException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import io.objectbox.InternalAccess;

public class SyncClientImpl implements SyncClient {

    private static final long LOGIN_TIMEOUT_SECONDS = 15;

    private final String url;
    private final boolean manualUpdateRequests;

    private volatile long handle;
    @Nullable private volatile SyncClientListener listener;
    @Nullable private volatile SyncChangesListener syncChangesListener;

    private boolean started;

    SyncClientImpl(SyncBuilder syncBuilder) {
        this.url = syncBuilder.url;
        long storeHandle = InternalAccess.getHandle(syncBuilder.boxStore);
        this.manualUpdateRequests = syncBuilder.manualUpdateRequests;

        handle = nativeCreate(storeHandle, url, syncBuilder.certificatePath);
        if (handle == 0) {
            throw new RuntimeException("Handle is zero");
        }

        SyncCredentials credentials = syncBuilder.credentials;
        byte[] credentialsBytes = SyncCredentialsToken.getTokenOrNull(credentials);
        nativeSetLoginInfo(handle, credentials.getTypeId(), credentialsBytes);
        credentials.clear();  // Clear immediately, not needed anymore
    }

    @Override
    public String url() {
        return url;
    }

    @Override
    public synchronized void setSyncListener(SyncClientListener listener) {
        checkNotNull(listener, "Listener must not be null. Use removeSyncListener to remove existing listener.");
        this.listener = listener;
    }

    @Override
    public synchronized void removeSyncListener() {
        this.listener = null;
    }

    @Override
    public void setSyncChangesListener(SyncChangesListener changesListener) {
        checkNotNull(changesListener, "Listener must not be null. Use removeSyncChangesListener to remove existing listener.");
        this.syncChangesListener = changesListener;
        if (handle != 0) {
            nativeSetSyncChangesListener(handle, changesListener);
        }
    }

    @Override
    public void removeSyncChangesListener() {
        this.syncChangesListener = null;
        if (handle != 0) {
            nativeSetSyncChangesListener(handle, null);
        }
    }

    public synchronized void awaitLogin(final ConnectCallback callback) {
        if (started) {
            throw new IllegalStateException("Already started");
        }

        try {
            // if listeners were set before connecting register them now
            if (syncChangesListener != null) {
                nativeSetSyncChangesListener(handle, syncChangesListener);
            }

            final CountDownLatch loginLatch = new CountDownLatch(1);
            // always set a SyncClientListener, forward to a user-set listener
            // We might be able to set the user listener natively in the near future; without our delegating listener
            nativeSetListener(handle, new SyncClientListener() {
                @Override
                public void onLogin(long response) {
                    loginLatch.countDown();

                    if (response == 20 /* OK */) {
                        if (!manualUpdateRequests) {
                            requestUpdates();
                        }
                        callback.onComplete(null);
                    } else {
                        callback.onComplete(new ConnectException("Failed to connect (code " + response + ")."));
                    }

                    SyncClientListener listenerToFire = listener;
                    if (listenerToFire != null) {
                        listenerToFire.onLogin(response);
                    }
                }

                @Override
                public void onSyncComplete() {
                    SyncClientListener listenerToFire = listener;
                    if (listenerToFire != null) {
                        listenerToFire.onSyncComplete();
                    }
                }
            });

            start();

            boolean onLoginCalled = loginLatch.await(LOGIN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!onLoginCalled) {
                close();
                callback.onComplete(new ConnectException("Failed to connect within " + LOGIN_TIMEOUT_SECONDS + " seconds."));
            }
        } catch (Exception e) {
            callback.onComplete(e);
        }
    }

    public synchronized void start() {
        if (handle == 0) return;
        nativeStart(handle);
        started = true;
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
    public synchronized void close() {
        long handleToDelete = this.handle;
        handle = 0;

        if(handleToDelete != 0) {
            nativeDelete(handleToDelete);
        }
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

    private native void nativeSetLoginInfo(long handle, long credentialsType, @Nullable byte[] credentials);

    private native void nativeSetListener(long handle, @Nullable SyncClientListener listener);

    private native void nativeSetSyncChangesListener(long handle, @Nullable SyncChangesListener advancedListener);

    /**
     * Request sync updates. Set {@code subscribeForPushes} to automatically receive updates for future changes.
     */
    private native void nativeRequestUpdates(long handle, boolean subscribeForPushes);

    /** (Optional) Cancel sync updates. */
    private native void nativeCancelUpdates(long handle);

}
