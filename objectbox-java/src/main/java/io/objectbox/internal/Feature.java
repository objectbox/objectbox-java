package io.objectbox.internal;

/**
 * Use with {@link io.objectbox.BoxStore#hasFeature(Feature)}.
 */
public enum Feature {

    /** Internal feature, not relevant if using ObjectBox through JNI. */
    RESULT_ARRAY(1),

    /** TimeSeries support (date/date-nano companion ID and other time-series functionality). */
    TIME_SERIES(2),

    /** Sync client availability. Visit <a href="https://objectbox.io/sync">the ObjectBox Sync website</a> for more details. */
    SYNC(3),

    /** Check whether debug log can be enabled during runtime. */
    DEBUG_LOG(4),

    /** HTTP server with a database browser. */
    ADMIN(5),

    /** Trees & GraphQL support */
    TREES(6),

    /** Embedded Sync server availability. */
    SYNC_SERVER(7);

    public final int id;

    Feature(int id) {
        this.id = id;
    }
}
