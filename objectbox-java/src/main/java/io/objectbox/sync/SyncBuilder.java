package io.objectbox.sync;

import javax.annotation.Nullable;

import io.objectbox.BoxStore;

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
     * By default, sync automatically requests updates from the backend; with this, you can override this behavior.
     * @see SyncClient#requestUpdates()
     */
    public SyncBuilder manualUpdateRequests() {
        manualUpdateRequests = true;
        return this;
    }


    /**
     * By default, sync automatically starts; with this, you can override this behavior.
     * @see SyncClient#start()
     */
    public SyncBuilder manualStart() {
        manualStart = true;
        return this;
    }

    /**
     * Sets the synchronization listener.
     * @see SyncClient#setSyncListener(SyncClientListener)
     */
    public SyncBuilder listener(SyncClientListener listener) {
        this.listener = listener;
        return this;
    }

    /**
     * Sets the synchronization listener.
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
