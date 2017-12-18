package io.objectbox.query;


import java.util.concurrent.Callable;

import io.objectbox.InternalAccess;
import io.objectbox.Property;

/** Query for a specific property; create using {@link Query#property(Property)}. */
@SuppressWarnings("WeakerAccess") // WeakerAccess: allow inner class access without accessor
public class PropertyQuery {
    final Query query;
    final Property property;
    boolean distinct;
    boolean noCaseIfDistinct = true;

    PropertyQuery(Query query, Property property) {
        this.query = query;
        this.property = property;
    }

    public PropertyQuery distinct() {
        distinct = true;
        return this;
    }

    public PropertyQuery distinct(QueryBuilder.StringOrder stringOrder) {
        distinct = true;
        noCaseIfDistinct = stringOrder == QueryBuilder.StringOrder.CASE_INSENSITIVE;
        return this;
    }

    /**
     * Find the values for the given string property for objects matching the query.
     * <p>
     * Note: this will list all strings (except null values), which may contain duplicates.
     *
     * @return Found strings
     */
    public String[] findStrings() {
        return (String[]) query.callInReadTx(new Callable<String[]>() {
            @Override
            public String[] call() {
                long cursorHandle = InternalAccess.getActiveTxCursorHandle(query.box);
                boolean distinctNoCase = distinct && noCaseIfDistinct;
                return query.nativeFindStrings(query.handle, cursorHandle, property.id, distinct, distinctNoCase);
            }
        });
    }

}
