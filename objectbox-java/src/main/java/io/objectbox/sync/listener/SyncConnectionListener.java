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

/**
 * Listens to sync connection events.
 */
public interface SyncConnectionListener {

    /**
     * Called when the client is disconnected from the sync server, e.g. due to a network error.
     * <p>
     * Depending on the configuration, the sync client typically tries to reconnect automatically,
     * triggering a {@link SyncLoginListener} again.
     */
    void onDisconnected();

}
