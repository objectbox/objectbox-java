package io.objectbox.sync;

/**
 * Returned by {@link io.objectbox.sync.SyncClientImpl#getSyncState()}.
 */
public enum SyncState {

    UNKNOWN(0),
    CREATED(1),
    STARTED(2),
    CONNECTED(3),
    LOGGED_IN(4),
    DISCONNECTED(5),
    STOPPED(6),
    DEAD(7);

    public final int id;

    SyncState(int id) {
        this.id = id;
    }

    public static SyncState fromId(int id) {
        for (SyncState value : values()) {
            if (value.id == id) return value;
        }
        return UNKNOWN;
    }

}
