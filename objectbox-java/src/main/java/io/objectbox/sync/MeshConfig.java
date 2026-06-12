/*
 * Copyright 2026 ObjectBox Ltd.
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
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import io.objectbox.annotation.apihint.Experimental;
import io.objectbox.annotation.apihint.Internal;

/**
 * Configuration of a peer-to-peer mesh sync.
 * <p>
 * A mesh sync enables peer-to-peer (P2P) synchronization between sync clients without a central server. Pass an
 * instance to {@link SyncBuilder#mesh(MeshConfig)} to attach a mesh sync to the client; it starts and stops together
 * with the client. Query the running mesh via {@link SyncClient#getMesh()}.
 * <p>
 * A mesh requires at least one platform-specific network (transport); networks are registered by ObjectBox platform
 * libraries (e.g. for Android), which also create this configuration. Thus, this class can not be created directly;
 * see the documentation of the platform library for how to obtain a mesh configuration.
 * <p>
 * Only the mesh ID is required; all other values are optional and fall back to the defaults of the ObjectBox native
 * library when not set.
 */
@Experimental
public final class MeshConfig {

    private final String meshId;
    @Nullable private final Integer maxConnectionCount;
    @Nullable private final Integer backoffMillis;
    @Nullable private final Integer evictionBackoffMillis;
    @Nullable private final Long randomSeed;
    @Nullable private final Integer requestTimeoutMillis;
    @Nullable private final Integer advertisingDelayMillis;
    @Nullable private final Integer connectDelayMillis;
    @Nullable private final Integer initialDiscoveryDurationSeconds;
    @Nullable private final Integer discoveryDurationSeconds;
    @Nullable private final Integer discoveryPauseSeconds;
    @Nullable private final Integer discoveryPauseJitterSeconds;
    @Nullable private final Integer txLogBatchSizeKb;
    @Nullable private final Integer txLogBatchMaxCount;

    private final List<Long> networkInternalHandles;

    MeshConfig(Builder builder) {
        this.meshId = builder.meshId;
        this.maxConnectionCount = builder.maxConnectionCount;
        this.backoffMillis = builder.backoffMillis;
        this.evictionBackoffMillis = builder.evictionBackoffMillis;
        this.randomSeed = builder.randomSeed;
        this.requestTimeoutMillis = builder.requestTimeoutMillis;
        this.advertisingDelayMillis = builder.advertisingDelayMillis;
        this.connectDelayMillis = builder.connectDelayMillis;
        this.initialDiscoveryDurationSeconds = builder.initialDiscoveryDurationSeconds;
        this.discoveryDurationSeconds = builder.discoveryDurationSeconds;
        this.discoveryPauseSeconds = builder.discoveryPauseSeconds;
        this.discoveryPauseJitterSeconds = builder.discoveryPauseJitterSeconds;
        this.txLogBatchSizeKb = builder.txLogBatchSizeKb;
        this.txLogBatchMaxCount = builder.txLogBatchMaxCount;
        this.networkInternalHandles = Collections.unmodifiableList(new ArrayList<>(builder.networkInternalHandles));
    }

    /**
     * The mesh network identifier (required); nodes with different IDs ignore each other.
     */
    public String getMeshId() {
        return meshId;
    }

    /**
     * Max number of simultaneous connections a peer can have to other peers (default: 3).
     */
    @Nullable
    public Integer getMaxConnectionCount() {
        return maxConnectionCount;
    }

    /**
     * Backoff time in milliseconds before retrying a failed connection (default: 10000).
     */
    @Nullable
    public Integer getBackoffMillis() {
        return backoffMillis;
    }

    /**
     * Backoff time in milliseconds between peer evictions (default: 30000).
     */
    @Nullable
    public Integer getEvictionBackoffMillis() {
        return evictionBackoffMillis;
    }

    /**
     * Seed for the random engine; 0 means use the current time (default: 0).
     */
    @Nullable
    public Long getRandomSeed() {
        return randomSeed;
    }

    /**
     * Timeout in milliseconds for a TX request from a peer before retrying from another (default: 5000).
     */
    @Nullable
    public Integer getRequestTimeoutMillis() {
        return requestTimeoutMillis;
    }

    /**
     * Delay in milliseconds before advertising starts after the mesh sync starts (default: 2000).
     */
    @Nullable
    public Integer getAdvertisingDelayMillis() {
        return advertisingDelayMillis;
    }

    /**
     * Minimum delay in milliseconds between two outgoing connection attempts (default: 1000).
     */
    @Nullable
    public Integer getConnectDelayMillis() {
        return connectDelayMillis;
    }

    /**
     * Duration in seconds of the initial discovery phase (default: 30; 0 means never stop by time).
     */
    @Nullable
    public Integer getInitialDiscoveryDurationSeconds() {
        return initialDiscoveryDurationSeconds;
    }

    /**
     * Duration in seconds of a standard (non-initial) discovery phase (default: 15; 0 means never stop by time).
     */
    @Nullable
    public Integer getDiscoveryDurationSeconds() {
        return discoveryDurationSeconds;
    }

    /**
     * Pause in seconds between two discovery phases (default: 45).
     */
    @Nullable
    public Integer getDiscoveryPauseSeconds() {
        return discoveryPauseSeconds;
    }

    /**
     * Random +/- jitter in seconds applied to the discovery pause (default: 15; must be &lt;= pause).
     */
    @Nullable
    public Integer getDiscoveryPauseJitterSeconds() {
        return discoveryPauseJitterSeconds;
    }

    /**
     * Soft cap in KB for the total TX log payload batched into a single TxLogData message (default: 100).
     */
    @Nullable
    public Integer getTxLogBatchSizeKb() {
        return txLogBatchSizeKb;
    }

    /**
     * Max number of TX logs to batch into a single TxLogData message (default: 1000).
     */
    @Nullable
    public Integer getTxLogBatchMaxCount() {
        return txLogBatchMaxCount;
    }

    /**
     * Handles to platform-specific native networks (transports) to register with the mesh sync.
     */
    @Internal
    List<Long> getNetworkInternalHandles() {
        return networkInternalHandles;
    }

    /**
     * A builder to create a {@link MeshConfig}; can not be created directly, see {@link MeshConfig} for details.
     * <p>
     * See the {@link MeshConfig} getters for documentation on each option.
     */
    @Experimental
    public static final class Builder {

        final String meshId;
        @Nullable Integer maxConnectionCount;
        @Nullable Integer backoffMillis;
        @Nullable Integer evictionBackoffMillis;
        @Nullable Long randomSeed;
        @Nullable Integer requestTimeoutMillis;
        @Nullable Integer advertisingDelayMillis;
        @Nullable Integer connectDelayMillis;
        @Nullable Integer initialDiscoveryDurationSeconds;
        @Nullable Integer discoveryDurationSeconds;
        @Nullable Integer discoveryPauseSeconds;
        @Nullable Integer discoveryPauseJitterSeconds;
        @Nullable Integer txLogBatchSizeKb;
        @Nullable Integer txLogBatchMaxCount;

        final List<Long> networkInternalHandles = new ArrayList<>();

        /**
         * Use {@link InternalSyncAccess#createMeshConfigBuilder(String)} instead.
         */
        Builder(String meshId) {
            if (meshId == null || meshId.isEmpty()) {
                throw new IllegalArgumentException("meshId must not be null or empty.");
            }
            this.meshId = meshId;
        }

        /**
         * Max number of simultaneous connections a peer can have to other peers (default: 3).
         * <p>
         * The default of 3 already provides mesh resilience through alternative paths. 4 may give better fault
         * tolerance, but at the cost of more radio activity. Values above 4 are not recommended. 2 is typically not
         * recommended unless you run into severe radio limitations. 1 would be a rare special case if you only want to
         * create pairs, not a mesh.
         */
        public Builder maxConnectionCount(int maxConnectionCount) {
            this.maxConnectionCount = maxConnectionCount;
            return this;
        }

        /**
         * Backoff time in milliseconds before retrying a failed connection (default: 10000).
         */
        public Builder backoffMillis(int backoffMillis) {
            this.backoffMillis = backoffMillis;
            return this;
        }

        /**
         * Backoff time in milliseconds between peer evictions (default: 30000).
         * <p>
         * When an incoming peer has 0 connections but we are full, we evict one existing peer to make room. This
         * backoff prevents frequent evictions.
         */
        public Builder evictionBackoffMillis(int evictionBackoffMillis) {
            this.evictionBackoffMillis = evictionBackoffMillis;
            return this;
        }

        /**
         * Seed for the random engine; 0 means use the current time (default: 0).
         */
        public Builder randomSeed(long randomSeed) {
            this.randomSeed = randomSeed;
            return this;
        }

        /**
         * Timeout in milliseconds for a TX request from a peer before retrying from another (default: 5000).
         */
        public Builder requestTimeoutMillis(int requestTimeoutMillis) {
            this.requestTimeoutMillis = requestTimeoutMillis;
            return this;
        }

        /**
         * Delay in milliseconds before advertising starts after the mesh sync starts (default: 2000).
         * <p>
         * Discovery always starts immediately; advertising is delayed to "stretch out" radio activity.
         */
        public Builder advertisingDelayMillis(int advertisingDelayMillis) {
            this.advertisingDelayMillis = advertisingDelayMillis;
            return this;
        }

        /**
         * Minimum delay in milliseconds between two outgoing connection attempts (default: 1000).
         */
        public Builder connectDelayMillis(int connectDelayMillis) {
            this.connectDelayMillis = connectDelayMillis;
            return this;
        }

        /**
         * Duration in seconds of the initial discovery phase (default: 30; 0 means never stop by time).
         */
        public Builder initialDiscoveryDurationSeconds(int initialDiscoveryDurationSeconds) {
            this.initialDiscoveryDurationSeconds = initialDiscoveryDurationSeconds;
            return this;
        }

        /**
         * Duration in seconds of a standard (non-initial) discovery phase (default: 15; 0 means never stop by time).
         */
        public Builder discoveryDurationSeconds(int discoveryDurationSeconds) {
            this.discoveryDurationSeconds = discoveryDurationSeconds;
            return this;
        }

        /**
         * Pause in seconds between two discovery phases (default: 45).
         */
        public Builder discoveryPauseSeconds(int discoveryPauseSeconds) {
            this.discoveryPauseSeconds = discoveryPauseSeconds;
            return this;
        }

        /**
         * Random +/- jitter in seconds applied to the discovery pause (default: 15; must be &lt;= pause).
         * 0 disables jitter.
         */
        public Builder discoveryPauseJitterSeconds(int discoveryPauseJitterSeconds) {
            this.discoveryPauseJitterSeconds = discoveryPauseJitterSeconds;
            return this;
        }

        /**
         * Soft cap in KB for the total TX log payload batched into a single TxLogData message (default: 100).
         */
        public Builder txLogBatchSizeKb(int txLogBatchSizeKb) {
            this.txLogBatchSizeKb = txLogBatchSizeKb;
            return this;
        }

        /**
         * Max number of TX logs to batch into a single TxLogData message (default: 1000).
         * Must be in the range (0, 100000].
         */
        public Builder txLogBatchMaxCount(int txLogBatchMaxCount) {
            this.txLogBatchMaxCount = txLogBatchMaxCount;
            return this;
        }

        /**
         * Adds a platform-specific native network (transport) to register with the mesh sync.
         * Use {@link InternalSyncAccess#addNetworkInternalHandle(Builder, long)} instead.
         */
        void addNetworkInternalHandle(long networkInternalHandle) {
            networkInternalHandles.add(networkInternalHandle);
        }

        /**
         * Builds and returns the {@link MeshConfig}.
         * <p>
         * Note: most validation is done by the native library once the sync client is built.
         */
        public MeshConfig build() {
            return new MeshConfig(this);
        }

    }

}
