package io.objectbox.sync;

/**
 * Returned by {@link io.objectbox.sync.SyncClientImpl#getSyncState()}.
 */
public enum SyncClientState {

    UNKNOWN(0),
    CREATED(1),
    STARTED(2),
    CONNECTED(3),
    LOGGED_IN(4),
    DISCONNECTED(5),
    STOPPED(6),
    DEAD(7);

    public final int id;

    SyncClientState(int id) {
        this.id = id;
    }

    public static SyncClientState fromId(int id) {
        for (SyncClientState value : values()) {
            if (value.id == id) return value;
        }
        return UNKNOWN;
    }

}
