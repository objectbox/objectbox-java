package io.objectbox.sync;

import io.objectbox.annotation.apihint.Experimental;

/**
 * Use the static helper methods to build Sync credentials,
 * for example {@link #sharedSecret(String) SyncCredentials.sharedSecret("secret")}.
 */
@SuppressWarnings("unused")
@Experimental
public class SyncCredentials {

    /**
     * Authenticate with a shared secret. This could be a passphrase, big number or randomly chosen bytes.
     * The string is expected to use UTF-8 characters.
     */
    public static SyncCredentials sharedSecret(String secret) {
        return new SyncCredentialsToken(CredentialsType.SHARED_SECRET, secret);
    }

    /**
     * Authenticate with a shared secret. This could be a passphrase, big number or randomly chosen bytes.
     */
    public static SyncCredentials sharedSecret(byte[] secret) {
        return new SyncCredentialsToken(CredentialsType.SHARED_SECRET, secret);
    }

    /**
     * Authenticate with a Google account ID token obtained via
     * <a href="https://developers.google.com/identity/sign-in/android/backend-auth" target="_top">Google Sign-In</a>.
     */
    public static SyncCredentials google(String idToken) {
        return new SyncCredentialsToken(CredentialsType.GOOGLE, idToken);
    }

    /**
     * No authentication, unsecured. Use only for development and testing purposes.
     */
    public static SyncCredentials none() {
        return new SyncCredentialsToken(CredentialsType.NONE);
    }

    public enum CredentialsType {
        // Note: this needs to match with CredentialsType in Core.

        NONE(0),

        SHARED_SECRET(1),

        GOOGLE(2);

        public final long id;

        CredentialsType(long id) {
            this.id = id;
        }
    }

    SyncCredentials() {
    }

}
