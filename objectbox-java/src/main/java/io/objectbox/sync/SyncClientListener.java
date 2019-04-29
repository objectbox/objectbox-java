package io.objectbox.sync;

public interface SyncClientListener {

    /**
     * Called each time a sync was completed.
     */
    void onSyncComplete();

}
