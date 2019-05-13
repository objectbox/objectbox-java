package io.objectbox.sync;

@SuppressWarnings({"unused"})
public interface SyncClientListener {

    /**
     * Called once the login process has completed.
     *
     * Possible response code values:
     *     OK = 20,
     *     CREDENTIALS_REJECTED = 43,
     *     UNKNOWN = 50,
     *     AUTH_UNREACHABLE = 53,
     *     BAD_VERSION = 55,
     *     CLIENT_ID_TAKEN = 61,
     */
    void onLogin(long response);

    /**
     * Called each time a sync was completed.
     */
    void onSyncComplete();

}
