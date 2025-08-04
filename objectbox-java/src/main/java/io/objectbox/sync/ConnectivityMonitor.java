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

package io.objectbox.sync;

import javax.annotation.Nullable;

/**
 * Used by {@link SyncClient} to observe connectivity changes.
 * <p>
 * Implementations are provided by a {@link io.objectbox.sync.internal.Platform platform}.
 */
public abstract class ConnectivityMonitor {

    @Nullable
    private SyncClient syncClient;

    void setObserver(SyncClient syncClient) {
        //noinspection ConstantConditions Annotations do not enforce non-null.
        if (syncClient == null) {
            throw new IllegalArgumentException("Sync client must not be null");
        }
        this.syncClient = syncClient;
        onObserverSet();
    }

    void removeObserver() {
        this.syncClient = null;
        onObserverRemoved();
    }

    /**
     * Called right after the observer was set.
     */
    public void onObserverSet() {
    }

    /**
     * Called right after the observer was removed.
     */
    public void onObserverRemoved() {
    }

    /**
     * Notifies the observer that a connection is available.
     * Implementers should call this once a working network connection is available.
     */
    public final void notifyConnectionAvailable() {
        SyncClient syncClient = this.syncClient;
        if (syncClient != null) {
            syncClient.notifyConnectionAvailable();
        }
    }

}
