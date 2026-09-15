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
import io.objectbox.Box;
import io.objectbox.TestEntity;
import io.objectbox.model.EntityFlags;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Writes to a sync-enabled entity type are rejected until a Sync client has been started for the store.
 * <p>
 * This is native behavior the Java docs do not spell out; the message text is asserted because it is
 * what users see and search for. That {@code stop()} leaves writes activated while {@code close()}
 * deactivates them again is more a quirk than intended behavior, but still documented here.
 * <p>
 * Note: the client is built for a loopback URL nothing listens on; it does not have to reach a server.
 */
public class SyncWriteActivationTest extends AbstractObjectBoxTest {

    @Override
    protected int testEntityFlags() {
        return EntityFlags.SYNC_ENABLED;
    }

    private void assertPutRejected(Box<TestEntity> box) {
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> box.put(createTestEntity("x", 1)));
        assertTrue(e.getMessage(), e.getMessage().contains("Can not modify object of sync-enabled type \"TestEntity\""));
        assertTrue(e.getMessage(), e.getMessage().contains("because sync has not been activated for this store"));
    }

    @Test
    public void putSyncEntity_requiresStartedClient() {
        Box<TestEntity> box = getTestEntityBox();

        // No client: rejected.
        assertPutRejected(box);

        SyncClient client = Sync.client(store)
                .url("ws://127.0.0.1")
                .credentials(SyncCredentials.none())
                .build();
        try {
            // Built, but not started: still rejected.
            assertPutRejected(box);

            // Started: writes are activated, even though there is no server.
            client.start();
            assertTrue(box.put(createTestEntity("x", 1)) > 0);

            // Stopped: writes stay activated.
            client.stop();
            assertTrue(box.put(createTestEntity("y", 2)) > 0);
        } finally {
            client.close();
        }

        // Closed: deactivated again.
        assertPutRejected(box);
    }
}
