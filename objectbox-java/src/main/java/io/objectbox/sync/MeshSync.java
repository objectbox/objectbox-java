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

import io.objectbox.annotation.apihint.Experimental;

/**
 * A running peer-to-peer mesh sync, obtained via {@link SyncClient#getMesh()}.
 * <p>
 * Configure a mesh via a {@link MeshConfig} passed to {@link SyncBuilder#mesh(MeshConfig)}. The mesh is owned by the
 * sync client and starts and stops together with it. Once the owning sync client is closed, this object is no longer
 * valid and any access throws an {@link IllegalStateException}.
 */
@Experimental
public final class MeshSync {

    /**
     * The native mesh is owned by the sync client; this handle is reset by {@link SyncClientImpl#close()} so later
     * access throws instead of using a dangling pointer.
     */
    private volatile long handle;

    MeshSync(long handle) {
        this.handle = handle;
    }

    private long getHandle() {
        long handle = this.handle;
        if (handle == 0) {
            throw new IllegalStateException("MeshSync is no longer valid (the owning SyncClient was closed)");
        }
        return handle;
    }

    /**
     * Invalidates this mesh; called by the owning sync client when it is closed.
     */
    void invalidate() {
        handle = 0;
    }

    /**
     * Gets the current state of this mesh sync.
     */
    public MeshState getState() {
        return MeshState.fromId(nativeGetState(getHandle()));
    }

    /**
     * Gets a human-readable string for the current mesh sync state (e.g. "Discovering").
     */
    public String getStateString() {
        return nativeGetStateString(getHandle());
    }

    /**
     * Returns the number of currently connected peers.
     */
    public long getConnectedPeerCount() {
        return nativeGetConnectedPeerCount(getHandle());
    }

    /**
     * Gets a mesh sync statistics counter value, see {@link MeshStats}.
     */
    public long getStats(MeshStats counter) {
        return nativeGetStats(getHandle(), counter.id);
    }

    /**
     * Requests an immediate retry of the network radios: advertising (bypassing the current retry
     * backoff) and discovery (restarting the current phase).
     * <p>
     * Call this when conditions that may have prevented the radios from starting have changed, e.g.
     * the user just granted the required permissions. Thread-safe; the actual retry happens on the
     * mesh sync thread shortly after.
     */
    public void retryNetworks() {
        nativeRetryNetworks(getHandle());
    }

    /**
     * Returns the current {@link MeshState} value.
     */
    private static native int nativeGetState(long handle);

    private static native String nativeGetStateString(long handle);

    private static native long nativeGetConnectedPeerCount(long handle);

    private static native void nativeRetryNetworks(long handle);

    /**
     * @param counterType One of the {@link MeshStats} IDs.
     */
    private static native long nativeGetStats(long handle, int counterType);

}
