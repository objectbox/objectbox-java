package io.objectbox.sync;

import org.junit.Test;

import io.objectbox.AbstractObjectBoxTest;
import io.objectbox.BoxStore;


import static org.junit.Assert.assertFalse;
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
    }

    @Test
    public void creatingSyncClient_throws() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> Sync.client(store, "wss://127.0.0.1", SyncCredentials.none())
        );
        String message = exception.getMessage();
        assertTrue(message, message.contains("does not include ObjectBox Sync") &&
                message.contains("https://objectbox.io/sync") && !message.contains("erver"));
    }

    @Test
    public void creatingSyncServer_throws() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> Sync.server(store, "wss://127.0.0.1", SyncCredentials.none())
        );
        String message = exception.getMessage();
        assertTrue(message, message.contains("does not include ObjectBox Sync Server") &&
                message.contains("https://objectbox.io/sync"));
    }
}
