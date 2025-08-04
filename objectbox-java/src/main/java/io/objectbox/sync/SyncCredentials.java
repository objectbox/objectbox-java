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
     * ObjectBox Admin user (username and password).
     */
    public static SyncCredentials obxAdminUser(String user, String password) {
        return new SyncCredentialsUserPassword(CredentialsType.OBX_ADMIN_USER, user, password);
    }

    /**
     * Generic credentials type suitable for ObjectBox Admin (and possibly others in the future).
     */
    public static SyncCredentials userAndPassword(String user, String password) {
        return new SyncCredentialsUserPassword(CredentialsType.USER_PASSWORD, user, password);
    }

    /**
     * Authenticate with a JSON Web Token (JWT) that is an ID token.
     * <p>
     * An ID token typically provides identity information about the authenticated user.
     * <p>
     * Use this and the other JWT methods that accept a token to configure JWT auth for a Sync client or server peer.
     * To configure Sync server auth options, use the server variants, like {@link #jwtIdTokenServer()}, instead.
     * <p>
     * See the <a href="https://sync.objectbox.io/sync-server-configuration/jwt-authentication">JWT authentication documentation</a>
     * for details.
     */
    public static SyncCredentials jwtIdToken(String jwtIdToken) {
        return new SyncCredentialsToken(CredentialsType.JWT_ID_TOKEN, jwtIdToken);
    }

    /**
     * Authenticate with a JSON Web Token (JWT) that is an access token.
     * <p>
     * An access token is used to access resources.
     * <p>
     * See {@link #jwtIdToken(String)} for some common remarks.
     */
    public static SyncCredentials jwtAccessToken(String jwtAccessToken) {
        return new SyncCredentialsToken(CredentialsType.JWT_ACCESS_TOKEN, jwtAccessToken);
    }

    /**
     * Authenticate with a JSON Web Token (JWT) that is a refresh token.
     * <p>
     * A refresh token is used to obtain a new access token.
     * <p>
     * See {@link #jwtIdToken(String)} for some common remarks.
     */
    public static SyncCredentials jwtRefreshToken(String jwtRefreshToken) {
        return new SyncCredentialsToken(CredentialsType.JWT_REFRESH_TOKEN, jwtRefreshToken);
    }

    /**
     * Authenticate with a JSON Web Token (JWT) that is neither an ID, access, nor refresh token.
     * <p>
     * See {@link #jwtIdToken(String)} for some common remarks.
     */
    public static SyncCredentials jwtCustomToken(String jwtCustomToken) {
        return new SyncCredentialsToken(CredentialsType.JWT_CUSTOM_TOKEN, jwtCustomToken);
    }

    /**
     * Enable authentication using a JSON Web Token (JWT) that is an ID token.
     * <p>
     * An ID token typically provides identity information about the authenticated user.
     * <p>
     * Use this and the other JWT server credentials types to configure a Sync server.
     * For Sync clients, use the ones that accept a token, like {@link #jwtIdToken(String)}, instead.
     * <p>
     * See the <a href="https://sync.objectbox.io/sync-server-configuration/jwt-authentication">JWT authentication documentation</a>
     * for details.
     */
    public static SyncCredentials jwtIdTokenServer() {
        return new SyncCredentialsToken(CredentialsType.JWT_ID_TOKEN);
    }

    /**
     * Enable authentication using a JSON Web Token (JWT) that is an access token.
     * <p>
     * An access token is used to access resources.
     * <p>
     * See {@link #jwtIdTokenServer()} for some common remarks.
     */
    public static SyncCredentials jwtAccessTokenServer() {
        return new SyncCredentialsToken(CredentialsType.JWT_ACCESS_TOKEN);
    }

    /**
     * Enable authentication using a JSON Web Token (JWT) that is a refresh token.
     * <p>
     * A refresh token is used to obtain a new access token.
     * <p>
     * See {@link #jwtIdTokenServer()} for some common remarks.
     */
    public static SyncCredentials jwtRefreshTokenServer() {
        return new SyncCredentialsToken(CredentialsType.JWT_REFRESH_TOKEN);
    }

    /**
     * Enable authentication using a JSON Web Token (JWT) that is neither an ID, access, nor refresh token.
     * <p>
     * See {@link #jwtIdTokenServer()} for some common remarks.
     */
    public static SyncCredentials jwtCustomTokenServer() {
        return new SyncCredentialsToken(CredentialsType.JWT_CUSTOM_TOKEN);
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
