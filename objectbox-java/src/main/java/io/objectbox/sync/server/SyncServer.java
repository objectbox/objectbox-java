package io.objectbox.sync.server;

import io.objectbox.sync.Sync;
import io.objectbox.sync.SyncChangesListener;

import java.io.Closeable;

/**
 * ObjectBox sync server. Build a server with {@link Sync#server}.
 */
@SuppressWarnings("unused")
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
     * Sets a {@link SyncChangesListener}. Replaces a previously set listener.
     */
    void setSyncChangesListener(SyncChangesListener listener);

    /**
     * Removes a previously set {@link SyncChangesListener}. Does nothing if no listener was set.
     */
    void removeSyncChangesListener();

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
