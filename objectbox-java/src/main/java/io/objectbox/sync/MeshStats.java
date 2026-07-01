/*
 * Copyright 2026 ObjectBox Ltd. <https://objectbox.io>
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

/**
 * Mesh sync statistics counters, useful for testing and diagnostics.
 * <p>
 * Read a counter value with {@link MeshSync#getStats(MeshStats)}.
 */
@Experimental
public enum MeshStats {

    /**
     * Number of peers discovered.
     */
    PEERS_DISCOVERED(1),

    /**
     * Number of peers connected.
     */
    PEERS_CONNECTED(2),

    /**
     * Number of peers disconnected.
     */
    PEERS_DISCONNECTED(3),

    /**
     * Number of peer connection attempts that failed.
     */
    PEER_CONNECTIONS_FAILED(4),

    /**
     * Number of peers lost (no longer available after discovery).
     */
    PEERS_LOST(5),

    /**
     * Number of messages received.
     */
    MESSAGES_RECEIVED(6),

    /**
     * Number of messages sent.
     */
    MESSAGES_SENT(7),

    /**
     * Number of TX IDs announced.
     */
    TX_IDS_ANNOUNCED(8),

    /**
     * Number of TX IDs requested (sent in TxLogRequest messages).
     */
    TX_IDS_REQUESTED(9),

    /**
     * Number of TX IDs received.
     */
    TX_IDS_RECEIVED(10),

    /**
     * Number of TX logs sent (in TxLogData messages).
     */
    TX_LOGS_SENT(11),

    /**
     * Number of TX logs received and stored (from TxLogData messages).
     */
    TX_LOGS_RECEIVED(12),

    /**
     * Number of TX logs applied to the local database.
     */
    TX_LOGS_APPLIED(13),

    /**
     * Number of protocol errors.
     */
    PROTOCOL_ERRORS(14),

    /**
     * Number of general errors.
     */
    GENERAL_ERRORS(15),

    /**
     * Number of peers evicted to make room for newcomers.
     */
    PEERS_EVICTED(16),

    /**
     * Number of discoveries of our own peer ID prefix that were ignored.
     */
    SELF_DISCOVERIES_IGNORED(17);

    /**
     * The counter type ID passed to the native API.
     */
    final int id;

    MeshStats(int id) {
        this.id = id;
    }

}
