/*
 * Copyright 2024 ObjectBox Ltd.
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

import io.objectbox.annotation.apihint.Internal;

/**
 * Internal credentials implementation for user and password authentication.
 * Use {@link SyncCredentials} to build credentials.
 */
@Internal
public final class SyncCredentialsUserPassword extends SyncCredentials {

    private final String username;
    private final String password;

    SyncCredentialsUserPassword(CredentialsType type, String username, String password) {
        super(type);
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    @Override
    SyncCredentials createClone() {
        return new SyncCredentialsUserPassword(getType(), this.username, this.password);
    }
}
