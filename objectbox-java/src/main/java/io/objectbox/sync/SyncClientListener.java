package io.objectbox.sync;

@SuppressWarnings({"unused"})
public interface SyncClientListener {

    /**
     * Called each time a sync was completed.
     */
    void onSyncComplete();

}
