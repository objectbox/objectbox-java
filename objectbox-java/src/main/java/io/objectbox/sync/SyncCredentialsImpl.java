package io.objectbox.sync;

import javax.annotation.Nullable;

class SyncCredentialsImpl extends SyncCredentials {

    @Nullable private final String token;
    private final CredentialsType type;

    SyncCredentialsImpl(@Nullable String token, CredentialsType type) {
        this.token = token;
        this.type = type;
    }

    @Nullable
    String getToken() {
        return token;
    }

    int getTypeId() {
        return type.id;
    }
}
