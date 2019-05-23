package io.objectbox.sync.server;

import io.objectbox.sync.SyncCredentials;

class PeerInfo {
    String url;
    SyncCredentials credentials;

    PeerInfo(String url, SyncCredentials credentials) {
        this.url = url;
        this.credentials = credentials;
    }
}
