package io.objectbox.sync;

import io.objectbox.annotation.apihint.Internal;

/**
 * Internal credentials implementation for user and password authentication.
 * Use {@link SyncCredentials} to build credentials.
 */
@Internal
public final class SyncCredentialsUserPassword extends SyncCredentials {

    private final String username;
    private final String password;

    SyncCredentialsUserPassword(String username, String password) {
        super(CredentialsType.USER_PASSWORD);
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
