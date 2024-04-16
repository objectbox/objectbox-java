/*
 * Copyright 2019-2020 ObjectBox Ltd. All rights reserved.
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

package io.objectbox.sync.server;

import java.io.Closeable;

import javax.annotation.Nullable;

import io.objectbox.annotation.apihint.Experimental;
import io.objectbox.sync.Sync;
import io.objectbox.sync.listener.SyncChangeListener;

/**
 * ObjectBox sync server. Build a server with {@link Sync#server}.
 */
@SuppressWarnings("unused")
@Experimental
public interface SyncServer extends Closeable {

    /**
     * Gets the URL the server is running at.
     */
    String getUrl();

    /**
     * Gets the port the server has bound to.
     */
    int getPort();

    /**
     * Returns if the server is up and running.
     */
    boolean isRunning();

    /**
     * Gets some statistics from the sync server.
     */
    String getStatsString();

    /**
     * Sets a {@link SyncChangeListener}. Replaces a previously set listener.
     * Set to {@code null} to remove the listener.
     */
    void setSyncChangeListener(@Nullable SyncChangeListener listener);

    /**
     * Starts the server (e.g. bind to port) and gets everything operational.
     */
    void start();

    /**
     * Stops the server.
     */
    void stop();

    /**
     * Closes and cleans up all resources used by this sync server.
     * It can no longer be used afterwards, build a new sync server instead.
     * Does nothing if this sync server has already been closed.
     */
    void close();

}
