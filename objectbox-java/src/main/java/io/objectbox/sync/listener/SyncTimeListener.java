package io.objectbox.sync.listener;

public interface SyncTimeListener {

    /**
     * Called when server time information is received on the client.
     */
    void onServerTimeUpdate(long serverTimeNanos);

}
