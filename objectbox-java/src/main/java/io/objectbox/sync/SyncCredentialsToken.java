/*
 * Copyright 2019-2025 ObjectBox Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

    public boolean hasToken() {
        return token != null;
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
     * Note that when the token is passed as a String, that String is removed from memory at the earliest with the next
     * garbage collector run.
     * <p>
     * Also note that while the token is removed from the Java heap, it is present on the native heap of the Sync
     * component using it.
     */
    public void clear() {
        cleared = true;
        byte[] tokenToClear = this.token;
        if (tokenToClear != null) {
            Arrays.fill(tokenToClear, (byte) 0);
        }
        this.token = null;
    }

    @Override
    SyncCredentialsToken createClone() {
        if (cleared) {
            throw new IllegalStateException("Cannot clone: credentials already have been cleared");
        }
        if (token == null) {
            return new SyncCredentialsToken(getType());
        } else {
            return new SyncCredentialsToken(getType(), Arrays.copyOf(token, token.length));
        }
    }
}
