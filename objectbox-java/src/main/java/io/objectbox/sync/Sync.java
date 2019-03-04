package io.objectbox.sync;

@SuppressWarnings("unused")
public class Sync {

    public static SyncBuilder url(String url) {
        return new SyncBuilder(url);
    }

    private Sync() {
    }
}
