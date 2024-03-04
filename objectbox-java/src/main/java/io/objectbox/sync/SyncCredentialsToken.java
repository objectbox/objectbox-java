package io.objectbox.sync;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import javax.annotation.Nullable;

import io.objectbox.annotation.apihint.Internal;

/**
 * Internal credentials implementation. Use {@link SyncCredentials} to build credentials.
 */
@Internal
public final class SyncCredentialsToken extends SyncCredentials {

    @Nullable private byte[] token;
    private volatile boolean cleared;

    SyncCredentialsToken(CredentialsType type) {
        super(type);
        this.token = null;
    }

    SyncCredentialsToken(CredentialsType type, byte[] token) {
        this(type);
        // Annotations do not guarantee non-null values
        //noinspection ConstantValue
        if (token == null || token.length == 0) {
            throw new IllegalArgumentException("Token must not be empty");
        }
        this.token = token;
    }

    SyncCredentialsToken(CredentialsType type, String token) {
        this(type, token.getBytes(StandardCharsets.UTF_8));
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
     * <p>
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

}
