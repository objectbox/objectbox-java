package io.objectbox.sync;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

public class SyncCredentialsToken extends SyncCredentials {

    private byte[] token;
    private volatile boolean cleared;

    private static byte[] asUtf8Bytes(String token) {
        try {
            return token.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] getTokenOrNull(SyncCredentials credentials) {
        if (credentials instanceof SyncCredentialsToken) {
            return ((SyncCredentialsToken) credentials).getToken();
        } else {
            return null;
        }
    }

    SyncCredentialsToken(CredentialsType type, String token) {
        this(type, asUtf8Bytes(token));
    }

    SyncCredentialsToken(CredentialsType type, byte[] token) {
        super(type);
        if (token == null || token.length == 0) {
            throw new IllegalArgumentException("Token must not be empty");
        }
        this.token = token;
    }

    public byte[] getToken() {
        if (cleared) {
            throw new IllegalStateException("Credentials already have been cleared");
        }
        return token;
    }

    /**
     * Clear after usage.
     */
    public void clear() {
        cleared = true;
        byte[] tokenToClear = this.token;
        if (tokenToClear != null) {
            Arrays.fill(tokenToClear, (byte) 0);
        }
        this.token = null;
    }
}
