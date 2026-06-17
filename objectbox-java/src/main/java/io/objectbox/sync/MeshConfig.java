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
 * Configuration of a peer-to-peer mesh sync.
 * <p>
 * A mesh sync enables peer-to-peer (P2P) synchronization between sync clients without a central
 * server. Pass an instance to {@link SyncBuilder#mesh(MeshConfig)} to attach a mesh sync to the
 * client; it starts and stops together with the client. Query the running mesh via
 * {@link SyncClient#getMesh()}.
 * <p>
 * A mesh requires at least one platform-specific network (transport); networks are registered by
 * ObjectBox platform libraries (e.g. for Android), which also create this configuration. Thus,
 * instances can not be created directly; see the documentation of the platform library for how to
 * obtain a mesh configuration.
 * <p>
 * Only the mesh ID is required; all other values are optional and fall back to the defaults of the
 * ObjectBox native library when not set. The values are read once when the sync client is created,
 * which is also when the native library validates them.
 */
@Experimental
public interface MeshConfig {

    /**
     * Maximum number of simultaneous connections a peer can have to other peers (default: 3).
     * <p>
     * The default of 3 already provides mesh resilience through alternative paths. 4 may give
     * better fault tolerance, but at the cost of more radio activity. Values above 4 are not
     * recommended. 2 is typically not recommended unless you run into severe radio limitations. 1
     * would be a rare special case if you only want to create pairs, not a mesh.
     */
    MeshConfig maxConnectionCount(int maxConnectionCount);

    /**
     * Backoff time in milliseconds before retrying a failed connection (default: 10000).
     */
    MeshConfig backoffMillis(int backoffMillis);

    /**
     * Backoff time in milliseconds between peer evictions (default: 30000).
     * <p>
     * When an incoming peer has 0 connections, but this peer is at
     * {@link #maxConnectionCount(int)}, this peer ends a connection to an existing peer, it
     * "evicts" that peer, to make room. This backoff prevents frequent evictions.
     */
    MeshConfig evictionBackoffMillis(int evictionBackoffMillis);

    /**
     * Timeout in milliseconds for a TX request from a peer before retrying from another (default:
     * 5000).
     */
    MeshConfig requestTimeoutMillis(int requestTimeoutMillis);

    /**
     * Delay in milliseconds before advertising starts after the mesh sync starts (default: 2000).
     * <p>
     * Discovery always starts immediately; advertising is delayed to "stretch out" radio activity.
     */
    MeshConfig advertisingDelayMillis(int advertisingDelayMillis);

    /**
     * Minimum delay in milliseconds between two outgoing connection attempts (default: 1000).
     */
    MeshConfig connectDelayMillis(int connectDelayMillis);

    /**
     * Duration in seconds of the initial discovery phase (default: 30; 0 means never stop by
     * time).
     *
     * @see #discoveryDurationSeconds(int)
     */
    MeshConfig initialDiscoveryDurationSeconds(int initialDiscoveryDurationSeconds);

    /**
     * Duration in seconds of a standard (non-initial) discovery phase (default: 15; 0 means never
     * stop by time).
     *
     * @see #initialDiscoveryDurationSeconds(int)
     */
    MeshConfig discoveryDurationSeconds(int discoveryDurationSeconds);

    /**
     * Pause in seconds between two discovery phases (default: 45).
     */
    MeshConfig discoveryPauseSeconds(int discoveryPauseSeconds);

    /**
     * Random +/- jitter in seconds applied to the discovery pause (default: 15; must be &lt;=
     * pause). 0 disables jitter.
     */
    MeshConfig discoveryPauseJitterSeconds(int discoveryPauseJitterSeconds);

    /**
     * Soft cap in KB for the total TX log payload batched into a single TxLogData message (default:
     * 100).
     */
    MeshConfig txLogBatchSizeKb(int txLogBatchSizeKb);

    /**
     * Maximum number of TX logs to batch into a single TxLogData message (default: 1000). Must be
     * in the range (0, 100000].
     */
    MeshConfig txLogBatchMaxCount(int txLogBatchMaxCount);

}
