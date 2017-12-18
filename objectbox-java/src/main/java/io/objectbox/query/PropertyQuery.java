package io.objectbox.query;


import java.util.concurrent.Callable;

import io.objectbox.InternalAccess;
import io.objectbox.Property;

/**
 * Query for a specific property; create using {@link Query#property(Property)}.
 * Note: Property values do currently not consider any order defined for the main {@link Query} object
 * (subject to change in a future version).
 */
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

    /**
     * Only distinct values should be returned (e.g. 1,2,3 instead of 1,1,2,3,3,3).
     * <p>
     * Note: strings default to case-insensitive comparision;
     * to change that call {@link #distinct(QueryBuilder.StringOrder)}.
     */
    public PropertyQuery distinct() {
        distinct = true;
        return this;
    }

    /**
     * For string properties you can specify {@link io.objectbox.query.QueryBuilder.StringOrder#CASE_SENSITIVE} if you
     * want to have case sensitive distinct values (e.g. returning "foo","Foo","FOO" instead of "foo").
     */
    public PropertyQuery distinct(QueryBuilder.StringOrder stringOrder) {
        if (property.type != String.class) {
            throw new RuntimeException("Reserved for string properties, but got " + property);
        }
        distinct = true;
        noCaseIfDistinct = stringOrder == QueryBuilder.StringOrder.CASE_INSENSITIVE;
        return this;
    }

    /**
     * Find the values for the given string property for objects matching the query.
     * <p>
     * Note: null values are excluded from results.
     * <p>
     * Note: results are not guaranteed to be in any particular order.
     * <p>
     * See also: {@link #distinct}, {@link #distinct(QueryBuilder.StringOrder)}
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
