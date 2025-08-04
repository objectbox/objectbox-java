/*
 * Copyright 2019-2020 ObjectBox Ltd.
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

import io.objectbox.annotation.apihint.Experimental;
import io.objectbox.sync.listener.SyncListener;

/**
 * Codes used by {@link SyncListener#onLoginFailed(long)}.
 */
@Experimental
public class SyncLoginCodes {

    public static final long OK = 20;
    public static final long REQ_REJECTED = 40;
    public static final long CREDENTIALS_REJECTED = 43;
    public static final long UNKNOWN = 50;
    public static final long AUTH_UNREACHABLE = 53;
    public static final long BAD_VERSION = 55;
    public static final long CLIENT_ID_TAKEN = 61;
    public static final long TX_VIOLATED_UNIQUE = 71;

    private SyncLoginCodes() {
    }
}
