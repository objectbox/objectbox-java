/*
 * Copyright 2020-2024 ObjectBox Ltd. All rights reserved.
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

import io.objectbox.exception.FeatureNotAvailableException;
import org.junit.Test;

import io.objectbox.AbstractObjectBoxTest;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class SyncTest extends AbstractObjectBoxTest {

    /**
     * Ensure that non-sync native library correctly reports sync client availability.
     * <p>
     * Note: this test is mirrored in objectbox-integration-test sync tests, where sync is available.
     */
    @Test
    public void clientIsNotAvailable() {
        assertFalse(Sync.isAvailable());
    }

    /**
     * Ensure that non-sync native library correctly reports sync server availability.
     * <p>
     * Note: this test is mirrored in objectbox-integration-test sync tests, where sync is available.
     */
    @Test
    public void serverIsNotAvailable() {
        assertFalse(Sync.isServerAvailable());
        assertFalse(Sync.isHybridAvailable());
    }

    @Test
    public void creatingSyncClient_throws() {
        FeatureNotAvailableException exception = assertThrows(
                FeatureNotAvailableException.class,
                () -> Sync.client(store, "wss://127.0.0.1", SyncCredentials.none())
        );
        String message = exception.getMessage();
        assertTrue(message, message.contains("does not include ObjectBox Sync") &&
                message.contains("https://objectbox.io/sync") && !message.contains("erver"));
    }

    @Test
    public void creatingSyncServer_throws() {
        FeatureNotAvailableException exception = assertThrows(
                FeatureNotAvailableException.class,
                () -> Sync.server(store, "wss://127.0.0.1", SyncCredentials.none())
        );
        String message = exception.getMessage();
        assertTrue(message, message.contains("does not include ObjectBox Sync Server") &&
                message.contains("https://objectbox.io/sync"));
    }

    @Test
    public void cloneSyncCredentials() {
        SyncCredentialsToken credentials = (SyncCredentialsToken) SyncCredentials.sharedSecret("secret");
        SyncCredentialsToken clonedCredentials = credentials.createClone();

        assertNotSame(credentials, clonedCredentials);
        assertArrayEquals(clonedCredentials.getTokenBytes(), credentials.getTokenBytes());
        credentials.clear();
        assertThrows(IllegalStateException.class, credentials::getTokenBytes);
        assertArrayEquals(clonedCredentials.getTokenBytes(), "secret".getBytes(StandardCharsets.UTF_8));
    }
}
