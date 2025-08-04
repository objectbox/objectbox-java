/*
 * Copyright 2020 ObjectBox Ltd.
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

package io.objectbox.sync.listener;

import io.objectbox.sync.SyncLoginCodes;

/**
 * Listens to login events.
 */
public interface SyncLoginListener {

    /**
     * Called on a successful login.
     * <p>
     * At this point the connection to the sync destination was established and
     * entered an operational state, in which data can be sent both ways.
     */
    void onLoggedIn();

    /**
     * Called on a login failure. One of {@link SyncLoginCodes}, but never {@link SyncLoginCodes#OK}.
     */
    void onLoginFailed(long syncLoginCode);

}
