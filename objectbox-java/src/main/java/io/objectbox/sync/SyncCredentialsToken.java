package io.objectbox.sync;

import io.objectbox.annotation.apihint.Internal;

import javax.annotation.Nullable;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

/**
 * Internal credentials implementation. Use {@link SyncCredentials} to build credentials.
 */
@Internal
public class SyncCredentialsToken extends SyncCredentials {

    private final CredentialsType type;
    @Nullable private byte[] token;
    private volatile boolean cleared;

    SyncCredentialsToken(CredentialsType type) {
        this.type = type;
        this.token = null;
    }

    SyncCredentialsToken(CredentialsType type, @SuppressWarnings("NullableProblems") byte[] token) {
        this(type);
        if (token == null || token.length == 0) {
            throw new IllegalArgumentException("Token must not be empty");
        }
        this.token = token;
    }

    SyncCredentialsToken(CredentialsType type, String token) {
        this(type, asUtf8Bytes(token));
    }

    public long getTypeId() {
        return type.id;
    }

    @Nullable
    public byte[] getTokenBytes() {
        if (cleared) {
            throw new IllegalStateException("Credentials already have been cleared");
        }
        return token;
    }

    /**
     * Clear after usage.
     *
     * Note that actual data is not removed from memory until the next garbage collector run.
     * Anyhow, the credentials are still kept in memory by the native component.
     */
    public void clear() {
        cleared = true;
        byte[] tokenToClear = this.token;
        if (tokenToClear != null) {
            Arrays.fill(tokenToClear, (byte) 0);
        }
        this.token = null;
    }

    private static byte[] asUtf8Bytes(String token) {
        try {
            //noinspection CharsetObjectCanBeUsed On Android not available until SDK 19.
            return token.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
