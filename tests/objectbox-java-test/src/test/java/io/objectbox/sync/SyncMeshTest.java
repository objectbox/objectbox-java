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

import org.junit.Test;

import io.objectbox.AbstractObjectBoxTest;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

/**
 * Does basic tests of the peer-to-peer mesh sync API.
 */
public class SyncMeshTest extends AbstractObjectBoxTest {

    private static final String TEST_MESH_ID = "test-mesh";
    private static final String TEST_SERVER_URL = "ws://127.0.0.1:9999";

    @Test
    public void meshConfig_setsAllValues() {
        MeshConfig config = InternalSyncAccess.createMeshConfig(TEST_MESH_ID)
                .maxConnectionCount(4)
                .backoffMillis(5000)
                .evictionBackoffMillis(20000)
                .requestTimeoutMillis(3000)
                .advertisingDelayMillis(1000)
                .connectDelayMillis(500)
                .initialDiscoveryDurationSeconds(20)
                .discoveryDurationSeconds(10)
                .discoveryPauseSeconds(30)
                .discoveryPauseJitterSeconds(10)
                .txLogBatchSizeKb(50)
                .txLogBatchMaxCount(500);
        InternalSyncAccess.addNetworkInternalHandle(config, 12345);

        assertEquals(TEST_MESH_ID, config.meshId);
        assertEquals(Integer.valueOf(4), config.maxConnectionCount);
        assertEquals(Integer.valueOf(5000), config.backoffMillis);
        assertEquals(Integer.valueOf(20000), config.evictionBackoffMillis);
        assertEquals(Integer.valueOf(3000), config.requestTimeoutMillis);
        assertEquals(Integer.valueOf(1000), config.advertisingDelayMillis);
        assertEquals(Integer.valueOf(500), config.connectDelayMillis);
        assertEquals(Integer.valueOf(20), config.initialDiscoveryDurationSeconds);
        assertEquals(Integer.valueOf(10), config.discoveryDurationSeconds);
        assertEquals(Integer.valueOf(30), config.discoveryPauseSeconds);
        assertEquals(Integer.valueOf(10), config.discoveryPauseJitterSeconds);
        assertEquals(Integer.valueOf(50), config.txLogBatchSizeKb);
        assertEquals(Integer.valueOf(500), config.txLogBatchMaxCount);
        assertEquals(1, config.networkInternalHandles.size());
        assertEquals(Long.valueOf(12345), config.networkInternalHandles.get(0));
    }

    @Test
    public void meshConfig_defaultsToNoValues() {
        MeshConfig config = InternalSyncAccess.createMeshConfig(TEST_MESH_ID);

        assertEquals(TEST_MESH_ID, config.meshId);
        assertNull(config.maxConnectionCount);
        assertNull(config.backoffMillis);
        assertNull(config.evictionBackoffMillis);
        assertNull(config.requestTimeoutMillis);
        assertNull(config.advertisingDelayMillis);
        assertNull(config.connectDelayMillis);
        assertNull(config.initialDiscoveryDurationSeconds);
        assertNull(config.discoveryDurationSeconds);
        assertNull(config.discoveryPauseSeconds);
        assertNull(config.discoveryPauseJitterSeconds);
        assertNull(config.txLogBatchSizeKb);
        assertNull(config.txLogBatchMaxCount);
        assertEquals(0, config.networkInternalHandles.size());
    }

    @Test
    public void meshConfig_emptyMeshId_throws() {
        assertThrows(IllegalArgumentException.class, () -> InternalSyncAccess.createMeshConfig(""));
        //noinspection DataFlowIssue
        assertThrows(IllegalArgumentException.class, () -> InternalSyncAccess.createMeshConfig(null));
    }

    @Test
    public void meshState_fromId_mapsKnownValues() {
        MeshState[] states = MeshState.values();
        for (MeshState state : states) {
            assertEquals(state, MeshState.fromId(state.id));
        }
    }

    @Test
    public void meshState_fromId_mapsUnknownValues() {
        assertEquals(MeshState.UNKNOWN, MeshState.fromId(-1));
        MeshState[] states = MeshState.values();
        assertEquals(MeshState.UNKNOWN, MeshState.fromId(states[states.length - 1].id + 1));
    }

    @Test
    public void clientWithMesh_getMeshWorks() {
        MeshConfig config = InternalSyncAccess.createMeshConfig(TEST_MESH_ID);

        try (SyncClient client = Sync.client(store)
                .url(TEST_SERVER_URL)
                .credentials(SyncCredentials.none())
                .mesh(config)
                .build()) {
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
        }
    }

    @Test
    public void clientWithoutMesh_getMeshReturnsNull() {
        try (SyncClient client = Sync.client(store)
                .url(TEST_SERVER_URL)
                .credentials(SyncCredentials.none())
                .build()) {
            assertNull(client.getMesh());
        }
    }

}
