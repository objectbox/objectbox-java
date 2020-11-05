package io.objectbox.sync;

import io.objectbox.annotation.apihint.Experimental;
import io.objectbox.sync.listener.SyncListener;

/**
 * Codes used by {@link SyncListener#onLoginFailed(long)}.
 */
@Experimental
public class SyncLoginCodes {

    public static final long OK = 20;
    public static final long REQ_REJECTED = 40;
    public static final long CREDENTIALS_REJECTED = 43;
    public static final long UNKNOWN = 50;
    public static final long AUTH_UNREACHABLE = 53;
    public static final long BAD_VERSION = 55;
    public static final long CLIENT_ID_TAKEN = 61;
    public static final long TX_VIOLATED_UNIQUE = 71;

    private SyncLoginCodes() {
    }
}
