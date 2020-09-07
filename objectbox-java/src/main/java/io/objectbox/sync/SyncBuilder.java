package io.objectbox.sync;

import javax.annotation.Nullable;

import io.objectbox.BoxStore;
import io.objectbox.annotation.apihint.Experimental;
import io.objectbox.sync.internal.Platform;

/**
 * A builder to create a {@link SyncClient}; the builder itself should be created via
 * {@link Sync#client(BoxStore, String, SyncCredentials)}.
 */
@Experimental
@SuppressWarnings({"unused", "WeakerAccess"})
public class SyncBuilder {

    final Platform platform;
    final BoxStore boxStore;
    final String url;
    final SyncCredentials credentials;

    SyncClientListener listener;
    SyncChangesListener changesListener;

    @Nullable
    String[] trustedCertPaths;
    boolean uncommittedAcks;

    RequestUpdatesMode requestUpdatesMode = RequestUpdatesMode.AUTO;

    public enum RequestUpdatesMode {
        /**
         * Once logged in, does not request any sync updates automatically.
         * <p>
         * Sync updates will have to be requested manually.
         *
         * @see SyncClient#requestUpdates()
         * @see SyncClient#requestUpdatesOnce()
         */
        MANUAL,

        /**
         * Once logged in, requests sync updates automatically including subsequent pushes for data changes.
         * This is the default.
         */
        AUTO,

        /**
         * Once logged in, requests updates automatically once without subsequent pushes for data changes.
         * <p>
         * After the initial sync update, further updates will have to be requested manually.
         *
         * @see SyncClient#requestUpdates()
         * @see SyncClient#requestUpdatesOnce()
         */
        AUTO_NO_PUSHES
    }

    public SyncBuilder(BoxStore boxStore, String url, SyncCredentials credentials) {
        checkNotNull(boxStore, "BoxStore is required.");
        checkNotNull(url, "Sync server URL is required.");
        checkNotNull(credentials, "Sync credentials are required.");
        if (!BoxStore.isSyncAvailable()) {
            throw new IllegalStateException(
                    "This ObjectBox library (JNI) does not include sync. Please update your dependencies.");
        }
        this.platform = Platform.findPlatform();
        this.boxStore = boxStore;
        this.url = url;
        this.credentials = credentials;
    }

    /**
     * Configures a custom set of directory or file paths to search for trusted certificates in.
     * The first path that exists will be used.
     * <p>
     * Using this option is not recommended in most cases, as by default the sync client uses
     * the certificate authorities trusted by the host platform.
     */
    public SyncBuilder trustedCertificates(String[] paths) {
        this.trustedCertPaths = paths;
        return this;
    }

    /**
     * Configure automatic sync updates from the server.
     * If automatic sync updates are turned off, they will need to be requested using the sync client.
     *
     * @see SyncClient#requestUpdates()
     * @see SyncClient#requestUpdatesOnce()
     */
    public SyncBuilder requestUpdatesMode(RequestUpdatesMode requestUpdatesMode) {
        this.requestUpdatesMode = requestUpdatesMode;
        return this;
    }

    /**
     * Turns on sending of uncommitted acks.
     */
    public SyncBuilder uncommittedAcks() {
        this.uncommittedAcks = true;
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

    /**
     * Builds and returns a Sync client ready to {@link SyncClient#start()}.
     */
    public SyncClient build() {
        return new SyncClientImpl(this);
    }

    /**
     * Builds, {@link SyncClient#start() starts} and returns a Sync client.
     */
    public SyncClient buildAndStart() {
        SyncClient syncClient = build();
        syncClient.start();
        return syncClient;
    }

    private void checkNotNull(Object object, String message) {
        //noinspection ConstantConditions Non-null annotation does not enforce, so check for null.
        if (object == null) {
            throw new IllegalArgumentException(message);
        }
    }

}
