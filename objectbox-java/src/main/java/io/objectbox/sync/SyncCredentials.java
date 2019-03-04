package io.objectbox.sync;

@SuppressWarnings("unused")
public class SyncCredentials {

    public static SyncCredentials apiKey(String apiKey) {
        ensureNotEmpty(apiKey, "API key");
        return new SyncCredentialsImpl(apiKey, AuthenticationType.API_KEY);
    }

    public static SyncCredentials google(String idToken) {
        ensureNotEmpty(idToken, "Google ID token");
        return new SyncCredentialsImpl(idToken, AuthenticationType.GOOGLE);
    }

    private static void ensureNotEmpty(String token, String name) {
        if (token == null || token.length() == 0) {
            throw new IllegalArgumentException(name + " must not be empty");
        }
    }

    SyncCredentials() {
    }

    public enum AuthenticationType {

        API_KEY("api_key"),

        GOOGLE("google");

        public String id;

        AuthenticationType(String id) {
            this.id = id;
        }
    }

}
