package io.objectbox.sync.server;

import io.objectbox.sync.SyncChangesListener;

import java.io.Closeable;

/** A sync server built by {@link SyncServerBuilder}. */
@SuppressWarnings("unused")
public interface SyncServer extends Closeable {

    /** Get the sync server URL. */
    String url();

    /**
     * Sets a {@link SyncChangesListener}. Replaces a previously set listener.
     */
    void setSyncChangesListener(SyncChangesListener listener);

    /**
     * Removes a previously set {@link SyncChangesListener}. Does nothing if no listener was set.
     */
    void removeSyncChangesListener();

    /** Actually starts the server (e.g. bind to port) and get everything operational. */
    void start();

    /** Stops the server */
    void stop();

    /** Destroys all native resources - do not use this object anymore after calling this! */
    void close();

    /** Get some statistics from the sync server */
    String getStatsString();

    /** Is the server up and running? */
    boolean isRunning();

    /** The port the server has bound to. */
    int getPort();

}
