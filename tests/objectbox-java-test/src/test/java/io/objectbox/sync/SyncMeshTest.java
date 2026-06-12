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

import org.junit.Test;

import io.objectbox.AbstractObjectBoxTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assume.assumeNoException;
import static org.junit.Assume.assumeTrue;

/**
 * Tests the peer-to-peer mesh sync API.
 * <p>
 * Tests that require the Sync feature are skipped when the native library does not include it
 * (like the one tests run against by default). They are mirrored in objectbox-integration-test
 * sync tests, where Sync is available.
 */
public class SyncMeshTest extends AbstractObjectBoxTest {

    @Test
    public void meshConfigBuilder_setsAllValues() {
        MeshConfig.Builder builder = InternalSyncAccess.createMeshConfigBuilder("test-mesh")
                .maxConnectionCount(4)
                .backoffMillis(5000)
                .evictionBackoffMillis(20000)
                .randomSeed(42)
                .requestTimeoutMillis(3000)
                .advertisingDelayMillis(1000)
                .connectDelayMillis(500)
                .initialDiscoveryDurationSeconds(20)
                .discoveryDurationSeconds(10)
                .discoveryPauseSeconds(30)
                .discoveryPauseJitterSeconds(10)
                .txLogBatchSizeKb(50)
                .txLogBatchMaxCount(500);
        InternalSyncAccess.addNetworkInternalHandle(builder, 12345);
        MeshConfig config = builder.build();

        assertEquals("test-mesh", config.getMeshId());
        assertEquals(Integer.valueOf(4), config.getMaxConnectionCount());
        assertEquals(Integer.valueOf(5000), config.getBackoffMillis());
        assertEquals(Integer.valueOf(20000), config.getEvictionBackoffMillis());
        assertEquals(Long.valueOf(42), config.getRandomSeed());
        assertEquals(Integer.valueOf(3000), config.getRequestTimeoutMillis());
        assertEquals(Integer.valueOf(1000), config.getAdvertisingDelayMillis());
        assertEquals(Integer.valueOf(500), config.getConnectDelayMillis());
        assertEquals(Integer.valueOf(20), config.getInitialDiscoveryDurationSeconds());
        assertEquals(Integer.valueOf(10), config.getDiscoveryDurationSeconds());
        assertEquals(Integer.valueOf(30), config.getDiscoveryPauseSeconds());
        assertEquals(Integer.valueOf(10), config.getDiscoveryPauseJitterSeconds());
        assertEquals(Integer.valueOf(50), config.getTxLogBatchSizeKb());
        assertEquals(Integer.valueOf(500), config.getTxLogBatchMaxCount());
        assertEquals(1, config.getNetworkInternalHandles().size());
        assertEquals(Long.valueOf(12345), config.getNetworkInternalHandles().get(0));
    }

    @Test
    public void meshConfigBuilder_defaultsToNoValues() {
        MeshConfig config = InternalSyncAccess.createMeshConfigBuilder("test-mesh").build();

        assertEquals("test-mesh", config.getMeshId());
        assertNull(config.getMaxConnectionCount());
        assertNull(config.getBackoffMillis());
        assertNull(config.getEvictionBackoffMillis());
        assertNull(config.getRandomSeed());
        assertNull(config.getRequestTimeoutMillis());
        assertNull(config.getAdvertisingDelayMillis());
        assertNull(config.getConnectDelayMillis());
        assertNull(config.getInitialDiscoveryDurationSeconds());
        assertNull(config.getDiscoveryDurationSeconds());
        assertNull(config.getDiscoveryPauseSeconds());
        assertNull(config.getDiscoveryPauseJitterSeconds());
        assertNull(config.getTxLogBatchSizeKb());
        assertNull(config.getTxLogBatchMaxCount());
        assertEquals(0, config.getNetworkInternalHandles().size());
    }

    @Test
    public void meshConfigBuilder_emptyMeshId_throws() {
        assertThrows(IllegalArgumentException.class, () -> InternalSyncAccess.createMeshConfigBuilder(""));
        //noinspection DataFlowIssue
        assertThrows(IllegalArgumentException.class, () -> InternalSyncAccess.createMeshConfigBuilder(null));
    }

    @Test
    public void meshState_fromId_mapsAllValues() {
        assertEquals(MeshState.UNKNOWN, MeshState.fromId(0));
        assertEquals(MeshState.CREATED, MeshState.fromId(1));
        assertEquals(MeshState.DISCOVERING, MeshState.fromId(2));
        assertEquals(MeshState.FULLY_CONNECTED, MeshState.fromId(3));
        assertEquals(MeshState.STOPPED, MeshState.fromId(4));
        assertEquals(MeshState.DEAD, MeshState.fromId(5));
        assertEquals(MeshState.UNKNOWN, MeshState.fromId(-1));
        assertEquals(MeshState.UNKNOWN, MeshState.fromId(42));
    }

    @Test
    public void clientWithMesh_getMeshWorks() {
        assumeTrue(Sync.isAvailable());

        MeshConfig config = InternalSyncAccess.createMeshConfigBuilder("test-mesh").build();
        SyncClient client = null;
        try {
            client = Sync.client(store)
                    .url("ws://127.0.0.1:9999")
                    .credentials(SyncCredentials.none())
                    .mesh(config)
                    .build();
        } catch (UnsatisfiedLinkError e) {
            assumeNoException("Native library does not include the mesh sync API", e);
        }

        try {
            MeshSync mesh = client.getMesh();
            assertNotNull(mesh);

            assertEquals(MeshState.CREATED, mesh.getState());
            assertEquals("Created", mesh.getStateString());
            assertEquals(0, mesh.getConnectedPeerCount());
            for (MeshStats counter : MeshStats.values()) {
                assertEquals(0, mesh.getStats(counter));
            }

            client.close();
            // The mesh is owned by the client, accessing it after close should throw
            assertThrows(IllegalStateException.class, mesh::getState);
            assertNull(this.store.getSyncClient());
        } finally {
            client.close();
        }
    }

    @Test
    public void clientWithoutMesh_getMeshReturnsNull() {
        assumeTrue(Sync.isAvailable());

        SyncClient client = Sync.client(store)
                .url("ws://127.0.0.1:9999")
                .credentials(SyncCredentials.none())
                .build();
        try {
            assertNull(client.getMesh());
        } catch (UnsatisfiedLinkError e) {
            assumeNoException("Native library does not include the mesh sync API", e);
        } finally {
            client.close();
        }
    }

}
