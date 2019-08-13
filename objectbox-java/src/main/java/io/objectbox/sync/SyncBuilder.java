package io.objectbox.sync;

import io.objectbox.BoxStore;

import javax.annotation.Nullable;

@SuppressWarnings({"unused", "WeakerAccess"})
public class SyncBuilder {

    final BoxStore boxStore;
    final String url;
    final SyncCredentials credentials;

    @Nullable String certificatePath;

    SyncClientListener listener;
    SyncChangesListener changesListener;

    boolean manualUpdateRequests;
    boolean manualStart;

    public SyncBuilder(BoxStore boxStore, String url, SyncCredentials credentials) {
        checkNotNull(boxStore, "BoxStore is required.");
        checkNotNull(url, "Sync server URL is required.");
        checkNotNull(credentials, "Sync credentials are required.");
        if (!BoxStore.isSyncAvailable()) {
            throw new IllegalStateException(
                    "This ObjectBox library (JNI) does not include sync. Please update your dependencies.");
        }
        this.boxStore = boxStore;
        this.url = url;
        this.credentials = credentials;
    }

    // TODO Check if this should remain exposed in the final API
    public SyncBuilder certificatePath(String certificatePath) {
        this.certificatePath = certificatePath;
        return this;
    }

    /**
     * Disables automatic sync updates from the server.
     * Sync updates will need to be enabled using the sync client.
     *
     * @see SyncClient#requestUpdates()
     * @see SyncClient#requestUpdatesOnce()
     */
    public SyncBuilder manualUpdateRequests() {
        manualUpdateRequests = true;
        return this;
    }


    /**
     * Prevents the client from starting (connecting, logging in, syncing) automatically.
     * It will need to be started manually later.
     *
     * @see SyncClient#start()
     */
    public SyncBuilder manualStart() {
        manualStart = true;
        return this;
    }

    /**
     * Sets a listener to observe sync events like login or sync completion.
     * This listener can also be set (or removed) on the sync client directly.
     *
     * @see SyncClient#setSyncListener(SyncClientListener)
     */
    public SyncBuilder listener(SyncClientListener listener) {
        this.listener = listener;
        return this;
    }

    /**
     * Sets a listener to observe fine granular changes happening during sync.
     * This listener can also be set (or removed) on the sync client directly.
     *
     * @see SyncClient#setSyncChangesListener(SyncChangesListener)
     */
    public SyncBuilder changesListener(SyncChangesListener changesListener) {
        this.changesListener = changesListener;
        return this;
    }

    public SyncClient build() {
        if (credentials == null) {
            throw new IllegalStateException("Credentials are required.");
        }
        return new SyncClientImpl(this);
    }

    private void checkNotNull(Object object, String message) {
        if (object == null) {
            throw new IllegalArgumentException(message);
        }
    }

}
