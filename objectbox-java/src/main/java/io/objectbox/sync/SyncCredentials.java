package io.objectbox.sync;

@SuppressWarnings("unused")
public class SyncCredentials {

    public static SyncCredentials none() {
        return new SyncCredentialsImpl(null, CredentialsType.NONE);
    }

    /** Authenticate with a pre-shared key. */
    public static SyncCredentials apiKey(String apiKey) {
        ensureNotEmpty(apiKey, "API key");
        return new SyncCredentialsImpl(apiKey, CredentialsType.API_KEY);
    }

    /**
     * Authenticate with a Google account ID token obtained via
     * <a href="https://developers.google.com/identity/sign-in/android/backend-auth" target="_top">Google Sign-In</a>.
     */
    public static SyncCredentials google(String idToken) {
        ensureNotEmpty(idToken, "Google ID token");
        return new SyncCredentialsImpl(idToken, CredentialsType.GOOGLE);
    }

    private static void ensureNotEmpty(String token, String name) {
        if (token == null || token.length() == 0) {
            throw new IllegalArgumentException(name + " must not be empty");
        }
    }

    SyncCredentials() {
    }

    public enum CredentialsType {
        // note: this needs to match with CredentialsType in Core

        NONE(0),

        API_KEY(1),

        GOOGLE(2);

        public long id;

        CredentialsType(long id) {
            this.id = id;
        }
    }

}
