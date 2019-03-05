package io.objectbox.sync;

import io.objectbox.BoxStore;

@SuppressWarnings("unused")
public class SyncBuilder {

    public final BoxStore boxStore;
    public final String url;
    public SyncCredentials credentials;

    public SyncBuilder(BoxStore boxStore, String url) {
        this.boxStore = boxStore;
        this.url = url;
    }

    public SyncBuilder credentials(SyncCredentials credentials) {
        this.credentials = credentials;
        return null;
    }

    public SyncClient build() {
        return new SyncClientImpl(this);
    }

}
