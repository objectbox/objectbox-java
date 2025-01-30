/*
 * Copyright 2019-2024 ObjectBox Ltd. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
import io.objectbox.sync.listener.SyncTimeListener;

/**
 * Internal sync client implementation. Use {@link SyncClient} to access functionality,
 * this class may change without notice.
 */
@Internal
public final class SyncClientImpl implements SyncClient {

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
    @Nullable
    private volatile SyncTimeListener timeListener;
    private volatile long lastLoginCode;
    private volatile boolean started;

    SyncClientImpl(SyncBuilder builder) {
        this.boxStore = builder.boxStore;
        this.serverUrl = builder.url;
        this.connectivityMonitor = builder.platform.getConnectivityMonitor();

        long boxStoreHandle = builder.boxStore.getNativeStore();
        long handle = nativeCreate(boxStoreHandle, serverUrl, builder.trustedCertPaths);
        if (handle == 0) {
            throw new RuntimeException("Failed to create sync client: handle is zero.");
        }
        this.handle = handle;

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
            this.timeListener = builder.timeListener;
        }

        this.internalListener = new InternalSyncClientListener();
        nativeSetListener(handle, internalListener);

        if (builder.credentials.size() == 1) {
            setLoginCredentials(builder.credentials.get(0));
        } else if (builder.credentials.size() > 1) {
            setLoginCredentials(builder.credentials.toArray(new SyncCredentials[0]));
        } else {
            throw new IllegalArgumentException("No credentials provided");
        }

        // If created successfully, let store keep a reference so the caller does not have to.
        InternalAccess.setSyncClient(builder.boxStore, this);
    }

    private long getHandle() {
        long handle = this.handle;
        if (handle == 0) {
            throw new IllegalStateException("SyncClient already closed");
        }
        return handle;
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
    public long getServerTimeNanos() {
        return nativeServerTime(getHandle());
    }

    @Override
    public long getServerTimeDiffNanos() {
        return nativeServerTimeDiff(getHandle());
    }

    @Override
    public long getRoundtripTimeNanos() {
        return nativeRoundtripTime(getHandle());
    }

    /**
     * Gets the current state of this sync client. Throws if {@link #close()} was called.
     */
    public SyncState getSyncState() {
        return SyncState.fromId(nativeGetState(getHandle()));
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
        nativeSetSyncChangesListener(getHandle(), changesListener);
    }

    @Override
    public void setSyncTimeListener(@Nullable SyncTimeListener timeListener) {
        this.timeListener = timeListener;
    }

    @Override
    public void setSyncConnectionListener(@Nullable SyncConnectionListener listener) {
        this.connectionListener = listener;
    }

    @Override
    public void setSyncListener(@Nullable SyncListener listener) {
        this.loginListener = listener;
        this.completedListener = listener;
        this.timeListener = listener;
        this.connectionListener = listener;
        setSyncChangeListener(listener);
    }

    @Override
    public void setLoginCredentials(SyncCredentials credentials) {
        if (credentials instanceof SyncCredentialsToken) {
            SyncCredentialsToken credToken = (SyncCredentialsToken) credentials;
            nativeSetLoginInfo(getHandle(), credToken.getTypeId(), credToken.getTokenBytes());
            credToken.clear(); // Clear immediately, not needed anymore.
        } else if (credentials instanceof SyncCredentialsUserPassword) {
            SyncCredentialsUserPassword credUserPassword = (SyncCredentialsUserPassword) credentials;
            nativeSetLoginInfoUserPassword(getHandle(), credUserPassword.getTypeId(), credUserPassword.getUsername(),
                    credUserPassword.getPassword());
        } else {
            throw new IllegalArgumentException("credentials is not a supported type");
        }
    }

    @Override
    public void setLoginCredentials(SyncCredentials[] multipleCredentials) {
        for (int i = 0; i < multipleCredentials.length; i++) {
            SyncCredentials credentials = multipleCredentials[i];
            boolean isLast = i == (multipleCredentials.length - 1);
            if (credentials instanceof SyncCredentialsToken) {
                SyncCredentialsToken credToken = (SyncCredentialsToken) credentials;
                nativeAddLoginCredentials(getHandle(), credToken.getTypeId(), credToken.getTokenBytes(), isLast);
                credToken.clear(); // Clear immediately, not needed anymore.
            } else if (credentials instanceof SyncCredentialsUserPassword) {
                SyncCredentialsUserPassword credUserPassword = (SyncCredentialsUserPassword) credentials;
                nativeAddLoginCredentialsUserPassword(getHandle(), credUserPassword.getTypeId(), credUserPassword.getUsername(),
                        credUserPassword.getPassword(), isLast);
            } else {
                throw new IllegalArgumentException("credentials is not a supported type");
            }
        }
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
        nativeStart(getHandle());
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
        nativeStop(getHandle());
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
        return nativeRequestFullSync(getHandle(), false);
    }

    /**
     * Temporary only, try not to use it.
     */
    @Experimental
    public boolean requestFullSyncAndUpdates() {
        return nativeRequestFullSync(getHandle(), true);
    }

    @Override
    public boolean requestUpdates() {
        return nativeRequestUpdates(getHandle(), true);
    }

    @Override
    public boolean requestUpdatesOnce() {
        return nativeRequestUpdates(getHandle(), false);
    }

    @Override
    public boolean cancelUpdates() {
        return nativeCancelUpdates(getHandle());
    }

    @Override
    public void notifyConnectionAvailable() {
        nativeTriggerReconnect(getHandle());
    }

    @Override
    public ObjectsMessageBuilder startObjectsMessage(long flags, @Nullable String topic) {
        return new ObjectsMessageBuilderImpl(this, flags, topic);
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

    private native void nativeSetLoginInfoUserPassword(long handle, long credentialsType, String username, String password);

    private native void nativeAddLoginCredentials(long handle, long credentialsType, @Nullable byte[] credentials, boolean complete);

    private native void nativeAddLoginCredentialsUserPassword(long handle, long credentialsType, String username, String password, boolean complete);

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
     * Hints to the native client that an active network connection is available.
     * Returns true if the native client was disconnected (and will try to re-connect).
     */
    private native boolean nativeTriggerReconnect(long handle);

    /**
     * The current server timestamp approximation based on the last server time
     * we've received and local steady clock.
     *
     * @return unix timestamp in nanoseconds
     */
    private native long nativeServerTime(long handle);

    /**
     * Returns the difference between the current local timestamp and the current
     * server timestamp approximation as given by nativeServerTime().
     * Equivalent to calculating: nanosSinceEpoch - nativeServerTime().
     *
     * @return unix timestamp difference in nanoseconds
     */
    private native long nativeServerTimeDiff(long handle);

    private native long nativeRoundtripTime(long handle);

    /**
     * Returns a handle to the message builder.
     *
     * @see #nativeObjectsMessageAddBytes
     * @see #nativeObjectsMessageAddString
     */
    private native long nativeObjectsMessageStart(long flags, @Nullable String topic);

    /**
     * @see #nativeObjectsMessageSend
     */
    private native void nativeObjectsMessageAddString(long builderHandle, long optionalId, String string);

    /**
     * @see #nativeObjectsMessageSend
     */
    private native void nativeObjectsMessageAddBytes(long builderHandle, long optionalId, byte[] bytes, boolean isFlatBuffer);

    /**
     * Do not use {@code builderHandle} afterwards.
     */
    private native boolean nativeObjectsMessageSend(long syncClientHandle, long builderHandle);

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

        public void onSyncComplete() {
            SyncCompletedListener listenerToFire = completedListener;
            if (listenerToFire != null) {
                listenerToFire.onUpdatesCompleted();
            }
        }

        public void onServerTimeUpdate(long serverTimeNanos) {
            SyncTimeListener listenerToFire = timeListener;
            if (listenerToFire != null) {
                listenerToFire.onServerTimeUpdate(serverTimeNanos);
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

    public static class ObjectsMessageBuilderImpl implements ObjectsMessageBuilder {
        private boolean sent;
        private final long builderHandle;
        private final SyncClientImpl syncClient;

        private ObjectsMessageBuilderImpl(SyncClientImpl syncClient, long flags, @Nullable String topic) {
            this.syncClient = syncClient;
            this.builderHandle = syncClient.nativeObjectsMessageStart(flags, topic);
        }

        @Override
        public ObjectsMessageBuilderImpl addString(long optionalId, String value) {
            checkNotSent();
            syncClient.nativeObjectsMessageAddString(builderHandle, optionalId, value);
            return this;
        }

        @Override
        public ObjectsMessageBuilderImpl addBytes(long optionalId, byte[] value, boolean isFlatBuffers) {
            checkNotSent();
            syncClient.nativeObjectsMessageAddBytes(builderHandle, optionalId, value, isFlatBuffers);
            return this;
        }

        @Override
        public boolean send() {
            if (!syncClient.isStarted()) {
                return false;
            }
            checkNotSent();
            sent = true;
            return syncClient.nativeObjectsMessageSend(syncClient.getHandle(), builderHandle);
        }

        private void checkNotSent() {
            if (sent) throw new IllegalStateException("Already sent this message, start a new one instead.");
        }
    }
}
