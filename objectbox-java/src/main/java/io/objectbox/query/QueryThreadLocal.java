package io.objectbox.query;

/**
 * A {@link ThreadLocal} that, given an original {@link Query} object,
 * returns a {@link Query#copy() copy}, for each thread.
 */
public class QueryThreadLocal<T> extends ThreadLocal<Query<T>> {

    private final Query<T> original;

    /**
     * See {@link QueryThreadLocal}.
     */
    public QueryThreadLocal(Query<T> original) {
        this.original = original;
    }

    @Override
    protected Query<T> initialValue() {
        return original.copy();
    }
}
