package io.objectbox.sync;

@SuppressWarnings({"unused"})
public interface SyncClientListener {

    /** Called on a successful login. */
    void onLogin();

    /**
     * Called on a login failure.
     *
     * Possible response code values:
     *     CREDENTIALS_REJECTED = 43,
     *     UNKNOWN = 50,
     *     AUTH_UNREACHABLE = 53,
     *     BAD_VERSION = 55,
     *     CLIENT_ID_TAKEN = 61,
     */
    void onLoginFailure(long response);

    /**
     * Called each time a sync was completed.
     */
    void onSyncComplete();

}
