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

import io.objectbox.annotation.apihint.Internal;

/**
 * Internal implementation of {@link MeshConfig}.
 * <p>
 * This class is not part of the public API and may change without notice.
 */
@Internal
public final class MeshConfigImpl implements MeshConfig {

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

    /**
     * Handles to platform-specific native networks (transports) to register with the mesh sync,
     * added via {@link InternalSyncAccess#addNetworkInternalHandle(MeshConfigImpl, long)}.
     */
    final List<Long> networkInternalHandles = new ArrayList<>();

    /**
     * Use {@link InternalSyncAccess#createMeshConfig(String)} instead.
     *
     * @param meshId The mesh network identifier (required); nodes with different IDs ignore each
     * other.
     */
    MeshConfigImpl(String meshId) {
        if (meshId == null || meshId.isEmpty()) {
            throw new IllegalArgumentException("meshId must not be null or empty.");
        }
        this.meshId = meshId;
    }

    @Override
    public MeshConfig maxConnectionCount(int maxConnectionCount) {
        this.maxConnectionCount = maxConnectionCount;
        return this;
    }

    @Override
    public MeshConfig backoffMillis(int backoffMillis) {
        this.backoffMillis = backoffMillis;
        return this;
    }

    @Override
    public MeshConfig evictionBackoffMillis(int evictionBackoffMillis) {
        this.evictionBackoffMillis = evictionBackoffMillis;
        return this;
    }

    /**
     * Seed for the random engine; 0 means use the current time (default: 0).
     * <p>
     * This is an internal-only method for testing not exposed via the {@link MeshConfig} interface.
     */
    public MeshConfigImpl randomSeed(long randomSeed) {
        this.randomSeed = randomSeed;
        return this;
    }

    @Override
    public MeshConfig requestTimeoutMillis(int requestTimeoutMillis) {
        this.requestTimeoutMillis = requestTimeoutMillis;
        return this;
    }

    @Override
    public MeshConfig advertisingDelayMillis(int advertisingDelayMillis) {
        this.advertisingDelayMillis = advertisingDelayMillis;
        return this;
    }

    @Override
    public MeshConfig connectDelayMillis(int connectDelayMillis) {
        this.connectDelayMillis = connectDelayMillis;
        return this;
    }

    @Override
    public MeshConfig initialDiscoveryDurationSeconds(int initialDiscoveryDurationSeconds) {
        this.initialDiscoveryDurationSeconds = initialDiscoveryDurationSeconds;
        return this;
    }

    @Override
    public MeshConfig discoveryDurationSeconds(int discoveryDurationSeconds) {
        this.discoveryDurationSeconds = discoveryDurationSeconds;
        return this;
    }

    @Override
    public MeshConfig discoveryPauseSeconds(int discoveryPauseSeconds) {
        this.discoveryPauseSeconds = discoveryPauseSeconds;
        return this;
    }

    @Override
    public MeshConfig discoveryPauseJitterSeconds(int discoveryPauseJitterSeconds) {
        this.discoveryPauseJitterSeconds = discoveryPauseJitterSeconds;
        return this;
    }

    @Override
    public MeshConfig txLogBatchSizeKb(int txLogBatchSizeKb) {
        this.txLogBatchSizeKb = txLogBatchSizeKb;
        return this;
    }

    @Override
    public MeshConfig txLogBatchMaxCount(int txLogBatchMaxCount) {
        this.txLogBatchMaxCount = txLogBatchMaxCount;
        return this;
    }

}

