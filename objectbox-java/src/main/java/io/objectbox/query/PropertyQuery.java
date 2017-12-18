package io.objectbox.query;


import java.util.concurrent.Callable;

import io.objectbox.InternalAccess;
import io.objectbox.Property;

/** TODO */
public class PropertyQuery {
    final Query query;
    final Property property;

    PropertyQuery(Query query, Property property) {
        this.query = query;
        this.property = property;
    }

    /**
     * Find the values for the given string property for objects matching the query.
     * <p>
     * Note: this will list all strings (except null values), which may contain duplicates.
     * Check {@link #findStringsUnique(QueryBuilder.StringOrder)} to avoid duplicates.
     *
     * @return Found strings
     */
    public String[] findStrings() {
        return (String[]) query.callInReadTx(new Callable<String[]>() {
            @Override
            public String[] call() {
                long cursorHandle = InternalAccess.getActiveTxCursorHandle(query.box);
                return query.nativeFindStrings(query.handle, cursorHandle, property.id, false, false);
            }
        });
    }

    /** Case-insensitive short-hand for {@link #findStringsUnique(QueryBuilder.StringOrder)}. */
    public String[] findStringsUnique() {
        return findStringsUnique(QueryBuilder.StringOrder.CASE_INSENSITIVE);
    }

    /**
     * Find the unique values for the given string property for objects matching the query.
     * <p>
     * Note: the order of returned strings may be completely random.
     *
     * @param stringOrder e.g. case sensitive/insensitive
     * @return Found strings
     */
    public String[] findStringsUnique(final QueryBuilder.StringOrder stringOrder) {
        return (String[]) query.callInReadTx(new Callable<String[]>() {
            @Override
            public String[] call() {
                long cursorHandle = InternalAccess.getActiveTxCursorHandle(query.box);
                boolean noCase = stringOrder == QueryBuilder.StringOrder.CASE_INSENSITIVE;
                return query.nativeFindStrings(query.handle, cursorHandle, property.id, true, noCase);
            }
        });
    }


}
