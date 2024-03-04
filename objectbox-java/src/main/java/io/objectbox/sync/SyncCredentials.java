package io.objectbox.sync;

/**
 * Use the static helper methods to build Sync credentials,
 * for example {@link #sharedSecret(String) SyncCredentials.sharedSecret("secret")}.
 */
@SuppressWarnings("unused")
public class SyncCredentials {

    private final CredentialsType type;

    /**
     * Authenticate with a shared secret. This could be a passphrase, big number or randomly chosen bytes.
     * The string is expected to use UTF-8 characters.
     */
    public static SyncCredentials sharedSecret(String secret) {
        return new SyncCredentialsToken(CredentialsType.SHARED_SECRET_SIPPED, secret);
    }

    /**
     * Authenticate with a shared secret. This could be a passphrase, big number or randomly chosen bytes.
     */
    public static SyncCredentials sharedSecret(byte[] secret) {
        return new SyncCredentialsToken(CredentialsType.SHARED_SECRET_SIPPED, secret);
    }

    /**
     * Authenticate with a Google account ID token obtained via
     * <a href="https://developers.google.com/identity/sign-in/android/backend-auth" target="_top">Google Sign-In</a>.
     */
    public static SyncCredentials google(String idToken) {
        return new SyncCredentialsToken(CredentialsType.GOOGLE, idToken);
    }

    public static SyncCredentials userAndPassword(String user, String password) {
        return new SyncCredentialsUserPassword(user, password);
    }

    /**
     * No authentication, unsecured. Use only for development and testing purposes.
     */
    public static SyncCredentials none() {
        return new SyncCredentialsToken(CredentialsType.NONE);
    }

    public enum CredentialsType {
        // Note: this needs to match with CredentialsType in Core.

        NONE(1),
        SHARED_SECRET(2),
        GOOGLE(3),
        SHARED_SECRET_SIPPED(4),
        OBX_ADMIN_USER(5),
        USER_PASSWORD(6);

        public final long id;

        CredentialsType(long id) {
            this.id = id;
        }
    }

    SyncCredentials(CredentialsType type) {
        this.type = type;
    }

    public long getTypeId() {
        return type.id;
    }

}
