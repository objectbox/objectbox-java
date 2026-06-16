/*
 * Copyright 2020-2025 ObjectBox Ltd.
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
import org.junit.function.ThrowingRunnable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.objectbox.AbstractObjectBoxTest;
import io.objectbox.exception.FeatureNotAvailableException;


import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * This subproject includes JVM libraries with the Sync feature (so client only). These tests verify
 * Java APIs behave as expected in that case.
 * <p>
 * The Java integration test repository has basic API tests that use JVM libraries without the Sync
 * feature as well as extended functional tests for Sync client and server.
 */
public class SyncTest extends AbstractObjectBoxTest {

    // Not specifying a port to use random available one to avoid collisions
    // Not using encrypted connection (wss protocol) as this requires a working certificate setup
    private static final String TEST_SERVER_URL = "ws://127.0.0.1";

    @Test
    public void clientIsAvailable() {
        assertTrue(Sync.isAvailable());
    }

    @Test
    public void serverIsNotAvailable() {
        assertFalse(Sync.isServerAvailable());
        assertFalse(Sync.isHybridAvailable());
    }

    @Test
    public void creatingSyncServer_throws() {
        FeatureNotAvailableException exception = assertThrows(
                FeatureNotAvailableException.class,
                () -> Sync.server(store, TEST_SERVER_URL, SyncCredentials.none())
        );
        String message = exception.getMessage();
        assertTrue(message, message.contains("does not include ObjectBox Sync Server") &&
                message.contains("https://objectbox.io/sync"));
    }

    /**
     * Can't re-use builder as credentials are cleared.
     */
    private SyncBuilder clientBuilder() {
        return Sync.client(store)
                .url(TEST_SERVER_URL)
                .credentials(SyncCredentials.none());
    }

    @Test
    public void clientReferenceIsKeptInStore() {
        assertNull(store.getSyncClient());

        // Once created, reference is added to store.
        try (SyncClient client = clientBuilder().build()) {
            assertNotNull(client);
            assertEquals(client, store.getSyncClient());

            // If created, creating again fails for same store.
            IllegalStateException exception = assertThrows(
                    IllegalStateException.class,
                    clientBuilder()::build
            );
            assertEquals(
                    "The given store is already associated with a Sync client, close it first.",
                    exception.getMessage()
            );
        }
        // Closing cleared reference from store
        assertNull(store.getSyncClient());

        // Creating again works after closing.
        try (SyncClient client = clientBuilder().build()) {
            assertNotNull(client);
            assertEquals(client, store.getSyncClient());
        }
    }

    @Test
    public void clientStates_noServer() {
        try (SyncClientImpl syncClient = (SyncClientImpl) clientBuilder().build()) {
            assertEquals(SyncState.CREATED, syncClient.getSyncState());

            syncClient.start();
            assertEquals(SyncState.STARTED, syncClient.getSyncState());

            syncClient.stop();
            assertEquals(SyncState.STOPPED, syncClient.getSyncState());
        }
    }

    @Test
    public void clientIsClosed_nativeMethodsThrow() {
        SyncClient syncClient = clientBuilder().build();

        syncClient.close();

        assertEquals(TEST_SERVER_URL, syncClient.getUrls().get(0));
        assertEquals(0, syncClient.getLastLoginCode());
        assertFalse(syncClient.isLoggedIn());
        syncClient.setSyncLoginListener(null);
        syncClient.setSyncCompletedListener(null);
        syncClient.setSyncTimeListener(null);
        syncClient.setSyncConnectionListener(null);
        assertFalse(syncClient.isStarted());

        assertThrows(IllegalStateException.class, syncClient::getServerTimeNanos);
        assertThrows(IllegalStateException.class, syncClient::getServerTimeDiffNanos);
        assertThrows(IllegalStateException.class, syncClient::getRoundtripTimeNanos);
        assertThrows(IllegalStateException.class, ((SyncClientImpl) syncClient)::getSyncState);
        assertThrows(IllegalStateException.class, () -> syncClient.setSyncChangeListener(null));
        assertThrows(IllegalStateException.class, () -> syncClient.setSyncListener(null));
        assertThrows(IllegalStateException.class, () -> syncClient.putFilterVariable("name", "value"));
        assertThrows(IllegalStateException.class, () -> syncClient.removeFilterVariable("name"));
        assertThrows(IllegalStateException.class, syncClient::removeAllFilterVariables);
        assertThrows(IllegalStateException.class, () -> syncClient.setLoginCredentials(SyncCredentials.none()));

        SyncCredentials[] multipleCredentials = new SyncCredentials[]{
                SyncCredentials.userAndPassword("user", "password"),
                SyncCredentials.jwtAccessToken("access-token")
        };
        assertThrows(IllegalStateException.class, () -> syncClient.setLoginCredentials(multipleCredentials));

        assertThrows(IllegalStateException.class, () -> syncClient.awaitFirstLogin(1));
        assertThrows(IllegalStateException.class, syncClient::start);
        assertThrows(IllegalStateException.class, syncClient::stop);
        assertThrows(IllegalStateException.class, syncClient::requestUpdates);
        assertThrows(IllegalStateException.class, syncClient::requestUpdatesOnce);
        assertThrows(IllegalStateException.class, syncClient::cancelUpdates);
        assertThrows(IllegalStateException.class, syncClient::notifyConnectionAvailable);

        // Calling close again shouldn't call native method again, so shouldn't throw.
        // Needs to behave this way as close() is also called by finalize().
        syncClient.close();
    }

    @Test
    public void syncClient_missingStore_throws() {
        //noinspection DataFlowIssue
        assertThrowsIsNull(() -> Sync.client(null));
    }

    @Test
    public void syncClient_missingUrl_throws() {
        // If a null URL is passed
        //noinspection DataFlowIssue
        assertThrowsIsNull(() -> Sync.client(store).url(null));
        List<String> nullUrl = new ArrayList<>();
        nullUrl.add(null);
        assertThrowsIsNull(() -> Sync.client(store).urls(nullUrl));
        //noinspection DataFlowIssue
        assertThrowsIsNull(() -> Sync.client(store).urls(null));

        // If no URLs are passed
        //noinspection resource
        IllegalArgumentException noUrls = assertThrows(IllegalArgumentException.class,
                () -> Sync.client(store).urls(Collections.emptyList()).build());
        assertEquals("At least one URL must be added to sync client options", noUrls.getMessage());
    }

    @Test
    public void syncClient_credentials() {
        // If no credentials are passed
        //noinspection resource
        IllegalArgumentException noCreds = assertThrows(IllegalArgumentException.class,
                () -> Sync.client(store).url(TEST_SERVER_URL).build());
        assertTrue(noCreds.getMessage().contains("Credentials must be provided"));
        //noinspection DataFlowIssue,resource
        assertThrowsIsNull(() -> Sync.client(store).url(TEST_SERVER_URL)
                .credentials((SyncCredentials) null).build());
        //noinspection DataFlowIssue,resource
        assertThrowsIsNull(() -> Sync.client(store).url(TEST_SERVER_URL)
                .credentials((List<SyncCredentials>) null).build());

        // If credentials are changed after creating the client
        try (SyncClient syncClient = clientBuilder().build()) {
            // Single credentials item
            //noinspection DataFlowIssue
            assertThrows(IllegalArgumentException.class,
                    () -> syncClient.setLoginCredentials((SyncCredentials) null));
            syncClient.setLoginCredentials(SyncCredentials.none());
            syncClient.setLoginCredentials(SyncCredentials.google("secret"));
            syncClient.setLoginCredentials(SyncCredentials.sharedSecret("secret"));
            syncClient.setLoginCredentials(SyncCredentials.none());

            // Multiple credentials items
            //noinspection DataFlowIssue
            assertThrows(IllegalArgumentException.class,
                    () -> syncClient.setLoginCredentials((SyncCredentials[]) null));
            SyncCredentials[] credentials = {
                    SyncCredentials.sharedSecret("<shared-secret>"),
                    SyncCredentials.jwtAccessToken("<jwt-access-token>")
            };
            syncClient.setLoginCredentials(credentials);
            syncClient.setLoginCredentials(new SyncCredentials[]{});
            IllegalArgumentException nullArrayItemEx = assertThrows(IllegalArgumentException.class,
                    () -> syncClient.setLoginCredentials(new SyncCredentials[]{
                            SyncCredentials.sharedSecret("<shared-secret>"),
                            null}));
            assertTrue(nullArrayItemEx.getMessage().contains("credentials is not a supported type"));
            // Note: the variant accepting a list forwards to the single item and array variant, so
            // just test the error case.
            IllegalArgumentException emptyListEx = assertThrows(IllegalArgumentException.class,
                    () -> syncClient.setLoginCredentials(new ArrayList<>()));
            assertTrue(emptyListEx.getMessage().contains("Credentials must be provided"));
        }
    }

    @Test
    public void credentials_cloningAndClearing_works() {
        SyncCredentialsToken credentials = (SyncCredentialsToken) SyncCredentials.sharedSecret("secret");
        SyncCredentialsToken clonedCredentials = credentials.createClone();

        assertNotSame(credentials, clonedCredentials);
        assertArrayEquals(clonedCredentials.getTokenBytes(), credentials.getTokenBytes());
        credentials.clear();
        assertThrows(IllegalStateException.class, credentials::getTokenBytes);
        assertArrayEquals(clonedCredentials.getTokenBytes(), "secret".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void syncClient_requestUpdatesMode() {
        try (SyncClient syncClient = clientBuilder()
                .requestUpdatesMode(SyncBuilder.RequestUpdatesMode.MANUAL)
                .build()) {
            // Try to change request updates mode while not connected.
            syncClient.start();
            // All below should return false, because the client is not connected.
            assertFalse(syncClient.requestUpdatesOnce());
            assertFalse(syncClient.requestUpdates());
            assertFalse(syncClient.cancelUpdates());
        }
    }

    @Test
    public void syncClient_filterVariables() {
        //noinspection DataFlowIssue
        assertThrowsIsNull(() -> clientBuilder().filterVariable(null, ""));
        //noinspection DataFlowIssue
        assertThrowsIsNull(() -> clientBuilder().filterVariable("", null));

        try (SyncClient syncClient = clientBuilder()
                .filterVariable("test-var-1", "test value 1")
                .build()) {
            syncClient.putFilterVariable("test-var-2", "test value 2");
            syncClient.removeFilterVariable("test-var-2");
            syncClient.putFilterVariable("test-var-2", "");
            syncClient.removeAllFilterVariables();
            syncClient.applyFilterVariables(); // no-op as client is not logged in

            assertThrows(IllegalArgumentException.class,
                    () -> syncClient.putFilterVariable("", "value"));
            //noinspection DataFlowIssue
            assertThrows(IllegalArgumentException.class,
                    () -> syncClient.putFilterVariable(null, "value"));
            //noinspection DataFlowIssue
            assertThrows(IllegalArgumentException.class,
                    () -> syncClient.putFilterVariable("name", null));
        }
    }

    @Test
    public void syncClient_trustedCertificates() {
        clientBuilder()
                .trustedCertificates(new String[]{})
                .build()
                .close();

        clientBuilder()
                .trustedCertificates(new String[]{
                        "/path/to/does-not-exist-1.crt",
                        "/path/to/does-not-exist-2.crt"
                })
                .build()
                .close();
    }

    @Test
    public void syncClient_flags() {
        clientBuilder()
                .flags(-1)
                .build()
                .close();

        clientBuilder()
                .flags(SyncFlags.DebugLogIdMapping
                        | SyncFlags.ClientKeepDataOnSyncError
                        | SyncFlags.DebugLogFilterVariables
                        | SyncFlags.RemoveWithObjectData
                        | SyncFlags.DebugLogTxLogs
                        | SyncFlags.SkipInvalidTxOps)
                .build().close();
    }

    private void assertThrowsIsNull(ThrowingRunnable runnable) {
        IllegalArgumentException isNullEx = assertThrows(IllegalArgumentException.class, runnable);
        assertTrue(isNullEx.getMessage().contains("must not be null"));
    }
}
