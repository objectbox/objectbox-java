/*
 * Copyright 2019-2024 ObjectBox Ltd. All rights reserved.
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

/**
 * Use the static helper methods to build Sync credentials,
 * for example {@link #sharedSecret(String) SyncCredentials.sharedSecret("secret")}.
 */
@SuppressWarnings("unused")
public abstract class SyncCredentials {

    private final CredentialsType type;

    /**
     * Authenticate with a shared secret. This could be a passphrase, big number or randomly chosen bytes.
     * The string is expected to use UTF-8 characters.
     */
    public static SyncCredentials sharedSecret(String secret) {
        return new SyncCredentialsToken(CredentialsType.SHARED_SECRET_SIPPED, secret);
    }

    /**
     * Authenticate with a shared secret. This could be a passphrase, big number or randomly chosen bytes.
     */
    public static SyncCredentials sharedSecret(byte[] secret) {
        return new SyncCredentialsToken(CredentialsType.SHARED_SECRET_SIPPED, secret);
    }

    /**
     * Authenticate with a Google account ID token obtained via
     * <a href="https://developers.google.com/identity/sign-in/android/backend-auth" target="_top">Google Sign-In</a>.
     */
    public static SyncCredentials google(String idToken) {
        return new SyncCredentialsToken(CredentialsType.GOOGLE, idToken);
    }

    /**
     * ObjectBox admin users (username/password)
     */
    public static SyncCredentials obxAdminUser(String user, String password) {
        return new SyncCredentialsUserPassword(CredentialsType.OBX_ADMIN_USER, user, password);
    }

    /**
     * Generic credential type suitable for ObjectBox admin (and possibly others in the future)
     */
    public static SyncCredentials userAndPassword(String user, String password) {
        return new SyncCredentialsUserPassword(CredentialsType.USER_PASSWORD, user, password);
    }

    /**
     * JSON Web Token (JWT): an ID token that typically provides identity information about the authenticated user.
     */
    public static SyncCredentials jwtIdToken(String jwtIdToken) {
        return new SyncCredentialsToken(CredentialsType.JWT_ID_TOKEN, jwtIdToken);
    }

    /**
     * JSON Web Token (JWT): an access token that is used to access resources.
     */
    public static SyncCredentials jwtAccessToken(String jwtAccessToken) {
        return new SyncCredentialsToken(CredentialsType.JWT_ACCESS_TOKEN, jwtAccessToken);
    }

    /**
     * JSON Web Token (JWT): a refresh token that is used to obtain a new access token.
     */
    public static SyncCredentials jwtRefreshToken(String jwtRefreshToken) {
        return new SyncCredentialsToken(CredentialsType.JWT_REFRESH_TOKEN, jwtRefreshToken);
    }

    /**
     * JSON Web Token (JWT): a token that is neither an ID, access, nor refresh token.
     */
    public static SyncCredentials jwtCustomToken(String jwtCustomToken) {
        return new SyncCredentialsToken(CredentialsType.JWT_CUSTOM_TOKEN, jwtCustomToken);
    }

    /**
     * No authentication, unsecured. Use only for development and testing purposes.
     */
    public static SyncCredentials none() {
        return new SyncCredentialsToken(CredentialsType.NONE);
    }

    public enum CredentialsType {

        NONE(io.objectbox.sync.CredentialsType.None),
        GOOGLE(io.objectbox.sync.CredentialsType.GoogleAuth),
        SHARED_SECRET_SIPPED(io.objectbox.sync.CredentialsType.SharedSecretSipped),
        OBX_ADMIN_USER(io.objectbox.sync.CredentialsType.ObxAdminUser),
        USER_PASSWORD(io.objectbox.sync.CredentialsType.UserPassword),
        JWT_ID_TOKEN(io.objectbox.sync.CredentialsType.JwtId),
        JWT_ACCESS_TOKEN(io.objectbox.sync.CredentialsType.JwtAccess),
        JWT_REFRESH_TOKEN(io.objectbox.sync.CredentialsType.JwtRefresh),
        JWT_CUSTOM_TOKEN(io.objectbox.sync.CredentialsType.JwtCustom);

        public final long id;

        CredentialsType(long id) {
            this.id = id;
        }
    }

    SyncCredentials(CredentialsType type) {
        this.type = type;
    }

    public CredentialsType getType() {
        return type;
    }

    public long getTypeId() {
        return type.id;
    }

    /**
     * Creates a copy of these credentials.
     * <p>
     * This can be useful to use the same credentials when creating multiple clients or a server in combination with a
     * client as some credentials may get cleared when building a client or server.
     */
    abstract SyncCredentials createClone();

}
