package io.objectbox.sync;

import javax.annotation.Nullable;

class SyncCredentialsImpl extends SyncCredentials {

    // TODO make this byte[] so we can overwrite memory on clear() for improved security
    @Nullable private String token;
    private final CredentialsType type;
    private volatile boolean cleared;

    SyncCredentialsImpl(@Nullable String token, CredentialsType type) {
        this.token = token;
        this.type = type;
    }

    @Nullable
    String getToken() {
        if(cleared) {
            throw new IllegalStateException("Credentials already have been cleared");
        }
        return token;
    }

    long getTypeId() {
        return type.id;
    }

    public void clear() {
        cleared = true;
        token = null;
    }
}
