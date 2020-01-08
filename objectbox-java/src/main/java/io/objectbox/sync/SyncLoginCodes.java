package io.objectbox.sync;

import io.objectbox.annotation.apihint.Experimental;

/**
 * Codes used by {@link SyncClientListener#onLoginFailure(long)}.
 */
@Experimental
public class SyncLoginCodes {

    public static final long OK = 20;
    public static final long CREDENTIALS_REJECTED = 43;
    public static final long UNKNOWN = 50;
    public static final long AUTH_UNREACHABLE = 53;
    public static final long BAD_VERSION = 55;
    public static final long CLIENT_ID_TAKEN = 61;

    private SyncLoginCodes() {
    }
}
