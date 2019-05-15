package io.objectbox.sync;

@SuppressWarnings("unused")
public class SyncCredentials {
    /**
     * No authentication - do not use in production or for anything other than developing / testing!
     */
    public static SyncCredentials none() {
        return new SyncCredentials(CredentialsType.NONE);
    }

    /**
     * Authenticate with a pre-shared key.
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

    private final CredentialsType type;

    SyncCredentials(CredentialsType type) {
        this.type = type;
    }

    public long getTypeId() {
        return type.id;
    }

    /** Clear after usage. */
    public void clear() {
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
