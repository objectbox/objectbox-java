package io.objectbox.sync;

class SyncCredentialsImpl extends SyncCredentials {

    private final String token;
    private final AuthenticationType type;

    SyncCredentialsImpl(String token, AuthenticationType type) {
        this.token = token;
        this.type = type;
    }

    public String getToken() {
        return token;
    }

    public String getTypeId() {
        return type.id;
    }
}
