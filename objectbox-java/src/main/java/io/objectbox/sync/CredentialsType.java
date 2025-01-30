/*
 * Copyright 2024 ObjectBox Ltd. All rights reserved.
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

// automatically generated by the FlatBuffers compiler, do not modify

package io.objectbox.sync;

/**
 * Credentials types for login at a sync server.
 */
@SuppressWarnings("unused")
public final class CredentialsType {
  private CredentialsType() { }
  /**
   * Used to indicate an uninitialized variable. Should never be sent/received in a message.
   */
  public static final int Invalid = 0;
  /**
   * No credentials required; do not use for public/production servers.
   * This is useful for testing and during development.
   */
  public static final int None = 1;
  /**
   * Deprecated, replaced by SHARED_SECRET_SIPPED
   */
  public static final int SharedSecret = 2;
  /**
   * Google Auth ID token
   */
  public static final int GoogleAuth = 3;
  /**
   * Use shared secret to create a SipHash and make attacks harder than just copy&paste.
   * (At some point we may want to switch to crypto & challenge/response.)
   */
  public static final int SharedSecretSipped = 4;
  /**
   * Use ObjectBox Admin users for Sync authentication.
   */
  public static final int ObxAdminUser = 5;
  /**
   * Generic credential type suitable for ObjectBox admin (and possibly others in the future)
   */
  public static final int UserPassword = 6;
  /**
   * JSON Web Token (JWT): an ID token that typically provides identity information about the authenticated user.
   */
  public static final int JwtId = 7;
  /**
   * JSON Web Token (JWT): an access token that is used to access resources.
   */
  public static final int JwtAccess = 8;
  /**
   * JSON Web Token (JWT): a refresh token that is used to obtain a new access token.
   */
  public static final int JwtRefresh = 9;
  /**
   * JSON Web Token (JWT): a token that is neither an ID, access, nor refresh token.
   */
  public static final int JwtCustom = 10;
}

