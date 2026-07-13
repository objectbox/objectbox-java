/*
 * Copyright 2026 ObjectBox Ltd. <https://objectbox.io>
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

import java.util.List;
import java.util.Map;
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
    private final List<String> urls;
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
    @Nullable
    private volatile MeshSync mesh;

    SyncClientImpl(SyncBuilder builder) {
        this.boxStore = builder.boxStore;
        this.urls = builder.urls;
        this.connectivityMonitor = builder.platform.getConnectivityMonitor();

        // Build the options
        long optHandle = nativeSyncOptCreate(builder.boxStore.getNativeStore());
        if (optHandle == 0) {
            throw new RuntimeException("Failed to create Sync client options: handle is zero.");
        }
        try {
            // Add all server URLs
            for (String url : urls) {
                nativeSyncOptAddUrl(optHandle, url);
            }

            // Add trusted certificate paths if provided
            if (builder.trustedCertPaths != null) {
                for (String certPath : builder.trustedCertPaths) {
                    nativeSyncOptAddCertPath(optHandle, certPath);
                }
            }

            // Add Sync flags if set
            if (builder.flags != 0) {
                nativeSyncOptFlags(optHandle, builder.flags);
            }

            // Attach a mesh sync configuration if set
            if (builder.meshConfig != null) {
                applyMeshConfig(optHandle, builder.meshConfig);
            }
        } catch (Exception e) {
            // Free the options if any option method call failed (like due to invalid arguments)
            nativeSyncOptFree(optHandle);
            throw e;
        }

        // Create the sync client (this frees the options in any case)
        long handle = nativeSyncOptCreateClient(optHandle);
        if (handle == 0) {
            throw new RuntimeException("Failed to create Sync client: handle is zero.");
        }
        this.handle = handle;

        for (Map.Entry<String, String> entry : builder.filterVariables.entrySet()) {
            putFilterVariable(entry.getKey(), entry.getValue());
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
            this.timeListener = builder.timeListener;
        }

        this.internalListener = new InternalSyncClientListener();
        nativeSetListener(handle, internalListener);

        setLoginCredentials(builder.credentials);

        // If created successfully, let store keep a reference so the caller does not have to.
        InternalAccess.setSyncClient(builder.boxStore, this);
    }

    /**
     * Builds native mesh options from the given config and attaches them to the sync options.
     * <p>
     * Only options with a value are passed to the native API, so the native defaults stay authoritative.
     * The native library validates the configuration when the sync client is created.
     */
    private static void applyMeshConfig(long syncOptHandle, MeshConfig config) {
        long meshOptHandle = nativeMeshOptCreate(config.meshId);
        if (meshOptHandle == 0) {
            throw new RuntimeException("Failed to create mesh options: handle is zero.");
        }
        try {
            if (config.maxConnectionCount != null) {
                nativeMeshOptMaxConnectionCount(meshOptHandle, config.maxConnectionCount);
            }
            if (config.backoffMillis != null) {
                nativeMeshOptBackoffMillis(meshOptHandle, config.backoffMillis);
            }
            if (config.evictionBackoffMillis != null) {
                nativeMeshOptEvictionBackoffMillis(meshOptHandle, config.evictionBackoffMillis);
            }
            if (config.requestTimeoutMillis != null) {
                nativeMeshOptRequestTimeoutMillis(meshOptHandle, config.requestTimeoutMillis);
            }
            if (config.advertisingDelayMillis != null) {
                nativeMeshOptAdvertisingDelayMillis(meshOptHandle, config.advertisingDelayMillis);
            }
            if (config.advertisingRetryMillis != null) {
                nativeMeshOptAdvertisingRetryMillis(meshOptHandle, config.advertisingRetryMillis);
            }
            if (config.advertisingRetryMaxMillis != null) {
                nativeMeshOptAdvertisingRetryMaxMillis(meshOptHandle, config.advertisingRetryMaxMillis);
            }
            if (config.connectDelayMillis != null) {
                nativeMeshOptConnectDelayMillis(meshOptHandle, config.connectDelayMillis);
            }
            if (config.initialDiscoveryDurationSeconds != null) {
                nativeMeshOptInitialDiscoveryDurationSeconds(meshOptHandle, config.initialDiscoveryDurationSeconds);
            }
            if (config.discoveryDurationSeconds != null) {
                nativeMeshOptDiscoveryDurationSeconds(meshOptHandle, config.discoveryDurationSeconds);
            }
            if (config.discoveryPauseSeconds != null) {
                nativeMeshOptDiscoveryPauseSeconds(meshOptHandle, config.discoveryPauseSeconds);
            }
            if (config.discoveryPauseJitterSeconds != null) {
                nativeMeshOptDiscoveryPauseJitterSeconds(meshOptHandle, config.discoveryPauseJitterSeconds);
            }
            if (config.txLogBatchSizeKb != null) {
                nativeMeshOptTxLogBatchSizeKb(meshOptHandle, config.txLogBatchSizeKb);
            }
            if (config.txLogBatchMaxCount != null) {
                nativeMeshOptTxLogBatchMaxCount(meshOptHandle, config.txLogBatchMaxCount);
            }
            for (long networkInternalHandle : config.networkInternalHandles) {
                nativeMeshOptNetworkInternal(meshOptHandle, networkInternalHandle);
            }
        } catch (Exception e) {
            // Free the mesh options if any option method call failed (like due to invalid arguments)
            nativeMeshOptFree(meshOptHandle);
            throw e;
        }
        // Attaches and consumes the mesh options (they are freed in any case, also on error)
        nativeSyncOptMesh(syncOptHandle, meshOptHandle);
    }

    private long getHandle() {
        long handle = this.handle;
        if (handle == 0) {
            throw new IllegalStateException("SyncClient already closed");
        }
        return handle;
    }

    @Override
    @Nullable
    public synchronized MeshSync getMesh() {
        MeshSync existingMesh = this.mesh;
        if (existingMesh != null) {
            return existingMesh;
        }

        long meshHandle = nativeGetMesh(getHandle());
        if (meshHandle == 0) return null;
        MeshSync mesh = new MeshSync(meshHandle);
        this.mesh = mesh;
        return mesh;
    }

    @Override
    public String getServerUrl() {
        // nativeSyncOptCreateClient guarantees there is at least one URL
        return getUrls().get(0);
    }

    @Override
    public List<String> getUrls() {
        return urls;
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

    public void putFilterVariable(String name, String value) {
        nativePutFilterVariable(getHandle(), name, value);
    }

    public void removeFilterVariable(String name) {
        nativeRemoveFilterVariable(getHandle(), name);
    }

    public void removeAllFilterVariables() {
        nativeRemoveAllFilterVariables(getHandle());
    }

    public void applyFilterVariables() {
        nativeApplyFilterVariables(getHandle());
    }

    @Override
    public void setLoginCredentials(List<SyncCredentials> credentials) {
        if (credentials.size() == 1) {
            setLoginCredentials(credentials.get(0));
        } else if (credentials.size() > 1) {
            setLoginCredentials(credentials.toArray(new SyncCredentials[0]));
        } else {
            throw new IllegalArgumentException("Credentials must be provided");
        }
    }

    @Override
    public void setLoginCredentials(SyncCredentials credentials) {
        if (credentials == null) {
            throw new IllegalArgumentException("credentials must not be null");
        }
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
        if (multipleCredentials == null) {
            throw new IllegalArgumentException("credentials must not be null");
        }
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

            // The native mesh is owned by the client and freed with it; invalidate any MeshSync wrapper so later
            // access throws instead of using a dangling pointer.
            MeshSync mesh = this.mesh;
            if (mesh != null) {
                mesh.invalidate();
                this.mesh = null;
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
     * Creates a sync client options object for the given store.
     * <p>
     * The options must be configured (at least one URL) and then used with {@link #nativeSyncOptCreateClient}.
     *
     * @return handle to the options object, or 0 on error
     */
    private static native long nativeSyncOptCreate(long storeHandle);

    /**
     * Adds a server URL to the sync options; at least one URL must be added before creating the sync client.
     * <p>
     * Passing multiple URLs allows high availability and load balancing (i.e. using an ObjectBox Sync Server Cluster).
     * <p>
     * A random URL is selected for each connection attempt.
     */
    private static native void nativeSyncOptAddUrl(long optHandle, String url);

    /**
     * Adds a certificate path to the sync options.
     * <p>
     * This allows to pass certificate paths referring to the local file system.
     * <p>
     * Example use cases are using self-signed certificates in a local development environment and custom certificate
     * authorities.
     */
    private static native void nativeSyncOptAddCertPath(long optHandle, String certPath);

    /**
     * Sets sync flags to adjust sync behavior; see SyncFlags for available flags.
     * <p>
     * Combine multiple flags using bitwise OR.
     */
    private static native void nativeSyncOptFlags(long optHandle, int flags);

    /**
     * Creates a sync client with the given options.
     * <p>
     * This does not initiate any connection attempts yet: call {@link #nativeStart} to do so. Before nativeStart(), you
     * must configure credentials via {@link #nativeSetLoginInfo} or {@link #nativeAddLoginCredentials}.
     * <p>
     * By default, a sync client automatically receives updates from the server once login succeeded. To configure this
     * differently, call {@link #nativeSetRequestUpdatesMode} with the wanted mode.
     * <p>
     * Note: the given options are always freed by this function, including when an error occurs.
     *
     * @return handle to the sync client, or 0 on error
     */
    private static native long nativeSyncOptCreateClient(long optHandle);

    /**
     * Frees the sync options object.
     * <p>
     * Note: Only free *unused* options; {@link #nativeSyncOptCreateClient} frees the options internally.
     */
    private static native void nativeSyncOptFree(long optHandle);

    /**
     * Creates a mesh options object used to configure a peer-to-peer mesh sync.
     * <p>
     * Pass it to {@link #nativeSyncOptMesh} to attach it to the sync options; that call frees the mesh options.
     *
     * @param meshId the mesh network identifier (required); nodes with different IDs ignore each other.
     * @return handle to the mesh options object, or 0 on error
     */
    private static native long nativeMeshOptCreate(String meshId);

    /**
     * Frees the mesh options object.
     * <p>
     * Note: Only free *unused* options; {@link #nativeSyncOptMesh} frees the mesh options internally.
     */
    private static native void nativeMeshOptFree(long meshOptHandle);

    private static native void nativeMeshOptMaxConnectionCount(long meshOptHandle, int count);

    private static native void nativeMeshOptBackoffMillis(long meshOptHandle, int millis);

    private static native void nativeMeshOptEvictionBackoffMillis(long meshOptHandle, int millis);

    private static native void nativeMeshOptRequestTimeoutMillis(long meshOptHandle, int millis);

    private static native void nativeMeshOptAdvertisingDelayMillis(long meshOptHandle, int millis);

    private static native void nativeMeshOptAdvertisingRetryMillis(long meshOptHandle, int millis);

    private static native void nativeMeshOptAdvertisingRetryMaxMillis(long meshOptHandle, int millis);

    private static native void nativeMeshOptConnectDelayMillis(long meshOptHandle, int millis);

    private static native void nativeMeshOptInitialDiscoveryDurationSeconds(long meshOptHandle, int seconds);

    private static native void nativeMeshOptDiscoveryDurationSeconds(long meshOptHandle, int seconds);

    private static native void nativeMeshOptDiscoveryPauseSeconds(long meshOptHandle, int seconds);

    private static native void nativeMeshOptDiscoveryPauseJitterSeconds(long meshOptHandle, int seconds);

    private static native void nativeMeshOptTxLogBatchSizeKb(long meshOptHandle, int sizeKb);

    private static native void nativeMeshOptTxLogBatchMaxCount(long meshOptHandle, int count);

    /**
     * Registers an internal mesh network (transport) implementation with the mesh options.
     * <p>
     * Networks are platform-specific (e.g. Android Nearby) and are created by ObjectBox platform libraries.
     *
     * @param networkInternalHandle an internal handle to a native mesh network.
     */
    private static native void nativeMeshOptNetworkInternal(long meshOptHandle, long networkInternalHandle);

    /**
     * Attaches a mesh sync configuration to the sync options.
     * <p>
     * When the sync client is created ({@link #nativeSyncOptCreateClient}), a mesh sync is created from this
     * configuration and attached to the client; it starts and stops together with the client. Use
     * {@link #nativeGetMesh} to query the running mesh.
     * <p>
     * Note: the given mesh options are always freed by this function, including when an error occurs.
     */
    private static native void nativeSyncOptMesh(long optHandle, long meshOptHandle);

    /**
     * Returns a handle to the mesh sync attached to the sync client (configured via {@link #nativeSyncOptMesh}),
     * or 0 if no mesh sync is attached.
     * <p>
     * The mesh sync is owned by the sync client; the handle is valid as long as the sync client is not closed.
     */
    private native long nativeGetMesh(long handle);

    private native void nativeDelete(long handle);

    private native void nativeStart(long handle);

    private native void nativeStop(long handle);

    // extern "C" JNIEXPORT void JNICALL Java_io_objectbox_sync_SyncClientImpl_nativePutFilterVariable(JNIEnv* env, jobject, jlong handle, jstring name, jstring value)
    private native void nativePutFilterVariable(long handle, String name, String value);

    // extern "C" JNIEXPORT void JNICALL Java_io_objectbox_sync_SyncClientImpl_nativeRemoveFilterVariable(JNIEnv* env, jobject, jlong handle, jstring name)
    private native void nativeRemoveFilterVariable(long handle, String name);

    // extern "C" JNIEXPORT void JNICALL Java_io_objectbox_sync_SyncClientImpl_nativeRemoveAllFilterVariables(JNIEnv* env, jobject, jlong handle)
    private native void nativeRemoveAllFilterVariables(long handle);

    // extern "C" JNIEXPORT void JNICALL Java_io_objectbox_sync_SyncClientImpl_nativeApplyFilterVariables(JNIEnv* env, jobject, jlong handle)
    private native void nativeApplyFilterVariables(long handle);

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
