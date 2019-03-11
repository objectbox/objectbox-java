package io.objectbox.sync;

import javax.annotation.Nullable;

import io.objectbox.BoxStore;

@SuppressWarnings("unused")
public class SyncBuilder {

    public final BoxStore boxStore;
    public final String objectBoxClientId;
    public final String url;
    @Nullable public String certificatePath;
    public SyncCredentials credentials;

    public SyncBuilder(BoxStore boxStore, String objectBoxClientId, String url) {
        checkNotNull(boxStore, "BoxStore is required.");
        checkNotNull(objectBoxClientId, "Sync client ID is required.");
        checkNotNull(url, "Sync server URL is required.");
        this.boxStore = boxStore;
        this.objectBoxClientId = objectBoxClientId;
        this.url = url;
    }

    public SyncBuilder certificatePath(String certificatePath) {
        this.certificatePath = certificatePath;
        return this;
    }

    public SyncBuilder credentials(SyncCredentials credentials) {
        this.credentials = credentials;
        return null;
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
