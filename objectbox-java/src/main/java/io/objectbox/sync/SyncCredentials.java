package io.objectbox.sync;

/**
 * Use the static helper methods to build sync credentials, for example {@link #apiKey SyncCredentials.apiKey("key")}.
 */
@SuppressWarnings("unused")
public class SyncCredentials {

    /**
     * Authenticate with a pre-shared key.
     *
     * @param apiKey will be UTF-8 encoded
     */
    public static SyncCredentials apiKey(String apiKey) {
        return new SyncCredentialsToken(CredentialsType.API_KEY, apiKey);
    }

    /**
     * Authenticate with a pre-shared key.
     */
    public static SyncCredentials apiKey(byte[] apiKey) {
        return new SyncCredentialsToken(CredentialsType.API_KEY, apiKey);
    }

    /**
     * Authenticate with a Google account ID token obtained via
     * <a href="https://developers.google.com/identity/sign-in/android/backend-auth" target="_top">Google Sign-In</a>.
     */
    public static SyncCredentials google(String idToken) {
        return new SyncCredentialsToken(CredentialsType.GOOGLE, idToken);
    }

    /**
     * No authentication, insecure. Use only for development and testing purposes.
     */
    public static SyncCredentials none() {
        return new SyncCredentialsToken(CredentialsType.NONE);
    }

    public enum CredentialsType {
        // Note: this needs to match with CredentialsType in Core.

        NONE(0),

        API_KEY(1),

        GOOGLE(2);

        public long id;

        CredentialsType(long id) {
            this.id = id;
        }
    }

    SyncCredentials() {
    }

}
