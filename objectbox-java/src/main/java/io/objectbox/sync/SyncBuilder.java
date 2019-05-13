package io.objectbox.sync;

import javax.annotation.Nullable;

import io.objectbox.BoxStore;

@SuppressWarnings("unused")
public class SyncBuilder {

    final BoxStore boxStore;
    final String url;
    boolean manualUpdateRequests;
    @Nullable String certificatePath;
    SyncCredentials credentials;

    public SyncBuilder(BoxStore boxStore, String url) {
        checkNotNull(boxStore, "BoxStore is required.");
        checkNotNull(url, "Sync server URL is required.");
        this.boxStore = boxStore;
        this.url = url;
    }

    // TODO Check if this should remain exposed in the final API
    public SyncBuilder certificatePath(String certificatePath) {
        this.certificatePath = certificatePath;
        return this;
    }

    public SyncBuilder credentials(SyncCredentials credentials) {
        this.credentials = credentials;
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
