package io.objectbox.sync;

@SuppressWarnings("unused")
public class SyncBuilder {

    private final String url;
    private SyncCredentials credentials;

    public SyncBuilder(String url) {
        this.url = url;
    }

    public SyncBuilder credentials(SyncCredentials credentials) {
        this.credentials = credentials;
        return null;
    }

    public void start() {
        // TODO
    }

}
