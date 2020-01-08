package io.objectbox.sync.server;

import io.objectbox.annotation.apihint.Experimental;
import io.objectbox.sync.SyncCredentials;

@Experimental
class PeerInfo {
    String url;
    SyncCredentials credentials;

    PeerInfo(String url, SyncCredentials credentials) {
        this.url = url;
        this.credentials = credentials;
    }
}
