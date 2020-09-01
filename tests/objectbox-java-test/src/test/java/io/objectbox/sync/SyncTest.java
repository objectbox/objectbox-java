package io.objectbox.sync;

import org.junit.Test;


import io.objectbox.BoxStore;


import static org.junit.Assert.assertFalse;

public class SyncTest {

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
}
