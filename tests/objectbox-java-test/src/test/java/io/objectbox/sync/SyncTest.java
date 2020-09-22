package io.objectbox.sync;

import org.junit.Test;

import io.objectbox.AbstractObjectBoxTest;
import io.objectbox.BoxStore;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;

public class SyncTest extends AbstractObjectBoxTest {

    /**
     * Ensure that non-sync native library correctly reports sync client availability.
     *
     * Note: this test is mirrored in objectbox-integration-test sync tests, where sync is available.
     */
    @Test
    public void clientIsNotAvailable() {
        assertFalse(Sync.isAvailable());
    }

    /**
     * Ensure that non-sync native library correctly reports sync server availability.
     *
     * Note: this test is mirrored in objectbox-integration-test sync tests, where sync is available.
     */
    @Test
    public void serverIsNotAvailable() {
        assertFalse(BoxStore.isSyncServerAvailable());
    }

    @Test
    public void creatingSyncClient_throws() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> Sync.client(store, "wss://127.0.0.1", SyncCredentials.none())
        );
        assertEquals("This ObjectBox library (JNI) does not include sync. Please update your dependencies.", exception.getMessage());
    }
}
