package io.objectbox.sync;

import io.objectbox.BoxStore;

@SuppressWarnings("unused")
public class Sync {

    public static SyncBuilder with(BoxStore boxStore, String syncClientId, String url) {
        return new SyncBuilder(boxStore, syncClientId, url);
    }

    private Sync() {
    }
}
