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

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import io.objectbox.annotation.apihint.Experimental;

/**
 * Configuration of a peer-to-peer mesh sync.
 * <p>
 * A mesh sync enables peer-to-peer (P2P) synchronization between sync clients without a central
 * server. Pass an instance to {@link SyncBuilder#mesh(MeshConfig)} to attach a mesh sync to the
 * client; it starts and stops together with the client. Query the running mesh via
 * {@link SyncClient#getMesh()}.
 * <p>
 * A mesh requires at least one platform-specific network (transport); networks are registered by
 * ObjectBox platform libraries (e.g. for Android), which also create this configuration. Thus, this
 * class can not be created directly; see the documentation of the platform library for how to
 * obtain a mesh configuration.
 * <p>
 * Only the mesh ID is required; all other values are optional and fall back to the defaults of the
 * ObjectBox native library when not set. The values are read once when the sync client is created,
 * which is also when the native library validates them.
 */
@Experimental
public final class MeshConfig {

    final String meshId;
    @Nullable Integer maxConnectionCount;
    @Nullable Integer backoffMillis;
    @Nullable Integer evictionBackoffMillis;
    @Nullable Integer requestTimeoutMillis;
    @Nullable Integer advertisingDelayMillis;
    @Nullable Integer advertisingRetryMillis;
    @Nullable Integer advertisingRetryMaxMillis;
    @Nullable Integer connectDelayMillis;
    @Nullable Integer initialDiscoveryDurationSeconds;
    @Nullable Integer discoveryDurationSeconds;
    @Nullable Integer discoveryPauseSeconds;
    @Nullable Integer discoveryPauseJitterSeconds;
    @Nullable Integer txLogBatchSizeKb;
    @Nullable Integer txLogBatchMaxCount;

    /**
     * Handles to platform-specific native networks (transports) to register with the mesh sync,
     * added via {@link InternalSyncAccess#addNetworkInternalHandle(MeshConfig, long)}.
     */
    final List<Long> networkInternalHandles = new ArrayList<>();

    /**
     * Use {@link InternalSyncAccess#createMeshConfig(String)} instead.
     *
     * @param meshId The mesh network identifier (required); nodes with different IDs ignore each
     * other.
     */
    MeshConfig(String meshId) {
        if (meshId == null || meshId.isEmpty()) {
            throw new IllegalArgumentException("meshId must not be null or empty.");
        }
        this.meshId = meshId;
    }

    /**
     * Maximum number of simultaneous connections a peer can have to other peers (default: 3).
     * <p>
     * The default of 3 already provides mesh resilience through alternative paths. 4 may give
     * better fault tolerance, but at the cost of more radio activity. Values above 4 are not
     * recommended. 2 is typically not recommended unless you run into severe radio limitations. 1
     * would be a rare special case if you only want to create pairs, not a mesh.
     */
    public MeshConfig maxConnectionCount(int maxConnectionCount) {
        this.maxConnectionCount = maxConnectionCount;
        return this;
    }

    /**
     * Backoff time in milliseconds before retrying a failed connection (default: 10000).
     */
    public MeshConfig backoffMillis(int backoffMillis) {
        this.backoffMillis = backoffMillis;
        return this;
    }

    /**
     * Backoff time in milliseconds between peer evictions (default: 30000).
     * <p>
     * When an incoming peer has 0 connections, but this peer is at
     * {@link #maxConnectionCount(int)}, this peer ends a connection to an existing peer, it
     * "evicts" that peer, to make room. This backoff prevents frequent evictions.
     */
    public MeshConfig evictionBackoffMillis(int evictionBackoffMillis) {
        this.evictionBackoffMillis = evictionBackoffMillis;
        return this;
    }

    /**
     * Timeout in milliseconds for a TX request from a peer before retrying from another (default:
     * 5000).
     */
    public MeshConfig requestTimeoutMillis(int requestTimeoutMillis) {
        this.requestTimeoutMillis = requestTimeoutMillis;
        return this;
    }

    /**
     * Delay in milliseconds before advertising starts after the mesh sync starts (default: 2000).
     * <p>
     * Discovery always starts immediately; advertising is delayed to "stretch out" radio activity.
     */
    public MeshConfig advertisingDelayMillis(int advertisingDelayMillis) {
        this.advertisingDelayMillis = advertisingDelayMillis;
        return this;
    }

    /**
     * Base delay in milliseconds before retrying advertising after a network failed to start it
     * (default: 5000).
     * <p>
     * A network may fail to start advertising (e.g. missing permissions); advertising is then
     * retried with exponential backoff (doubling up to {@link #advertisingRetryMaxMillis(int)}),
     * because the required permissions may be granted later at runtime. Must be positive.
     */
    public MeshConfig advertisingRetryMillis(int advertisingRetryMillis) {
        this.advertisingRetryMillis = advertisingRetryMillis;
        return this;
    }

    /**
     * Upper bound in milliseconds for the advertising retry backoff (default: 60000).
     * <p>
     * Must be greater than or equal to {@link #advertisingRetryMillis(int)}.
     */
    public MeshConfig advertisingRetryMaxMillis(int advertisingRetryMaxMillis) {
        this.advertisingRetryMaxMillis = advertisingRetryMaxMillis;
        return this;
    }

    /**
     * Minimum delay in milliseconds between two outgoing connection attempts (default: 1000).
     */
    public MeshConfig connectDelayMillis(int connectDelayMillis) {
        this.connectDelayMillis = connectDelayMillis;
        return this;
    }

    /**
     * Duration in seconds of the initial discovery phase (default: 30; 0 means never stop by
     * time).
     *
     * @see #discoveryDurationSeconds(int)
     */
    public MeshConfig initialDiscoveryDurationSeconds(int initialDiscoveryDurationSeconds) {
        this.initialDiscoveryDurationSeconds = initialDiscoveryDurationSeconds;
        return this;
    }

    /**
     * Duration in seconds of a standard (non-initial) discovery phase (default: 15; 0 means never
     * stop by time).
     *
     * @see #initialDiscoveryDurationSeconds(int)
     */
    public MeshConfig discoveryDurationSeconds(int discoveryDurationSeconds) {
        this.discoveryDurationSeconds = discoveryDurationSeconds;
        return this;
    }

    /**
     * Pause in seconds between two discovery phases (default: 45).
     */
    public MeshConfig discoveryPauseSeconds(int discoveryPauseSeconds) {
        this.discoveryPauseSeconds = discoveryPauseSeconds;
        return this;
    }

    /**
     * Random +/- jitter in seconds applied to the discovery pause (default: 15; must be &lt;=
     * pause). 0 disables jitter.
     */
    public MeshConfig discoveryPauseJitterSeconds(int discoveryPauseJitterSeconds) {
        this.discoveryPauseJitterSeconds = discoveryPauseJitterSeconds;
        return this;
    }

    /**
     * Soft cap in KB for the total TX log payload batched into a single TxLogData message (default:
     * 100).
     */
    public MeshConfig txLogBatchSizeKb(int txLogBatchSizeKb) {
        this.txLogBatchSizeKb = txLogBatchSizeKb;
        return this;
    }

    /**
     * Maximum number of TX logs to batch into a single TxLogData message (default: 1000). Must be
     * in the range (0, 100000].
     */
    public MeshConfig txLogBatchMaxCount(int txLogBatchMaxCount) {
        this.txLogBatchMaxCount = txLogBatchMaxCount;
        return this;
    }

}
