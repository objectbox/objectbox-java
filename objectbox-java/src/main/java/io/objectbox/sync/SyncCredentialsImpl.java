package io.objectbox.sync;

class SyncCredentialsImpl extends SyncCredentials {

    private final String token;
    private final CredentialsType type;

    SyncCredentialsImpl(String token, CredentialsType type) {
        this.token = token;
        this.type = type;
    }

    public String getToken() {
        return token;
    }

    public int getTypeId() {
        return type.id;
    }
}
