package io.objectbox.query;

import io.objectbox.Box;
import io.objectbox.Property;
import io.objectbox.annotation.apihint.Experimental;
import io.objectbox.annotation.apihint.Internal;
import io.objectbox.model.OrderFlags;

/**
 * With QueryBuilder you define custom queries returning matching entities. Using the methods of this class you can
 * select (filter) results for specific data (for example #{@link #equal(Property, String)} and
 * {@link #isNull(Property)}) and select an sort order for the resulting list (see {@link #order(Property)} and its overloads).
 * <p>
 * Use {@link #build()} to conclude your query definitions and to get a {@link Query} object, which is used to actually get results.
 * <p>
 * Note: Currently you can only query for complete entities. Returning individual property values or aggregates are
 * currently not available. Keep in mind that ObjectBox is very fast and the overhead to create an entity is very low.
 *
 * @param <T> Entity class associated with this query builder.
 */
@Experimental
public class QueryBuilder<T> {
    public enum StringOrder {
        /** The default: case insensitive ASCII characters */
        CASE_INSENSITIVE,

        /** Case matters ('a' != 'A'). */
        CASE_SENSITIVE
    }

    /**
     * Reverts the order from ascending (default) to descending.
     */
    public final static int DESCENDING = OrderFlags.DESCENDING;

    /**
     * Makes upper case letters (e.g. "Z") be sorted before lower case letters (e.g. "a").
     * If not specified, the default is case insensitive for ASCII characters.
     */
    public final static int CASE_SENSITIVE = OrderFlags.CASE_SENSITIVE;

    /**
     * null values will be put last.
     * If not specified, by default null values will be put first.
     */
    public final static int NULLS_LAST = OrderFlags.NULLS_LAST;

    /**
     * null values should be treated equal to zero (scalars only).
     */
    public final static int NULLS_ZERO = OrderFlags.NULLS_ZERO;

    /**
     * For scalars only: changes the comparison to unsigned (default is signed).
     */
    public final static int UNSIGNED = OrderFlags.UNSIGNED;

    private final Box<T> box;

    private long handle;

    private static native long nativeCreate(long storeHandle, String entityName);

    private static native long nativeDestroy(long handle);

    private static native long nativeBuild(long handle);

    private static native void nativeOrder(long handle, int propertyId, int flags);

    // ------------------------------ (Not)Null------------------------------

    private static native long nativeNull(long handle, int propertyId);

    private static native long nativeNotNull(long handle, int propertyId);

    // ------------------------------ Integers ------------------------------

    private static native long nativeEqual(long handle, int propertyId, long value);

    private static native long nativeNotEqual(long handle, int propertyId, long value);

    private static native long nativeLess(long handle, int propertyId, long value);

    private static native long nativeGreater(long handle, int propertyId, long value);

    private static native long nativeBetween(long handle, int propertyId, long value1, long value2);

    private static native long nativeIn(long handle, int propertyId, int[] values);

    private static native long nativeIn(long handle, int propertyId, long[] values);

    // ------------------------------ Strings ------------------------------

    private static native long nativeEqual(long handle, int propertyId, String value, boolean caseSensitive);

    private static native long nativeNotEqual(long handle, int propertyId, String value, boolean caseSensitive);

    private static native long nativeContains(long handle, int propertyId, String value, boolean caseSensitive);

    private static native long nativeStartsWith(long handle, int propertyId, String value, boolean caseSensitive);

    private static native long nativeEndsWith(long handle, int propertyId, String value, boolean caseSensitive);

    // ------------------------------ FPs ------------------------------
    private static native long nativeLess(long handle, int propertyId, double value);

    private static native long nativeGreater(long handle, int propertyId, double value);

    private static native long nativeBetween(long handle, int propertyId, double value1, double value2);

    @Internal
    public QueryBuilder(Box<T> box, long storeHandle, String entityName) {
        this.box = box;

        // This ensures that all properties have been set
        box.getProperties();

        handle = nativeCreate(storeHandle, entityName);
    }

    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

    public void close() {
        if (handle != 0) {
            nativeDestroy(handle);
            handle = 0;
        }
    }

    /**
     * Builds the query and closes this QueryBuilder.
     */
    public Query<T> build() {
        if (handle == 0) {
            throw new IllegalStateException("This QueryBuilder has already been closed. Please use a new instance.");
        }
        long queryHandle = nativeBuild(handle);
        Query<T> query = new Query<T>(box, queryHandle);
        close();
        return query;
    }

    /**
     * Specifies given property to be used for sorting.
     * Shorthand for {@link #order(Property, int)} with flags equal to 0.
     *
     * @see #order(Property, int)
     * @see #orderDesc(Property)
     */
    public QueryBuilder<T> order(Property property) {
        return order(property, 0);
    }

    /**
     * Specifies given property in descending order to be used for sorting.
     * Shorthand for {@link #order(Property, int)} with flags equal to {@link #DESCENDING}.
     *
     * @see #order(Property, int)
     * @see #order(Property)
     */
    public QueryBuilder<T> orderDesc(Property property) {
        return order(property, DESCENDING);
    }

    /**
     * Defines the order with which the results are ordered (default: none).
     * You can chain multiple order conditions. The first applied order condition will be the most relevant.
     * Order conditions applied afterwards are only relevant if the preceding ones resulted in value equality.
     * <p>
     * Example:
     * <p>
     * queryBuilder.order(Name).orderDesc(YearOfBirth);
     * <p>
     * Here, "Name" defines the primary sort order. The secondary sort order "YearOfBirth" is only used to compare
     * entries with the same "Name" values.
     *
     * @param property the property defining the order
     * @param flags    Bit flags that can be combined using the binary OR operator (|). Available flags are
     *                 {@link #DESCENDING}, {@link #CASE_SENSITIVE}, {@link #NULLS_LAST}, {@link #NULLS_ZERO},
     *                 and {@link #UNSIGNED}.
     * @see #order(Property)
     * @see #orderDesc(Property)
     */
    public QueryBuilder<T> order(Property property, int flags) {
        nativeOrder(handle, property.getId(), flags);
        return this;
    }

    public QueryBuilder<T> isNull(Property property) {
        nativeNull(handle, property.getId());
        return this;
    }

    public QueryBuilder<T> notNull(Property property) {
        nativeNotNull(handle, property.getId());
        return this;
    }

    public QueryBuilder<T> equal(Property property, long value) {
        nativeEqual(handle, property.getId(), value);
        return this;
    }

    public QueryBuilder<T> notEqual(Property property, long value) {
        nativeNotEqual(handle, property.getId(), value);
        return this;
    }

    public QueryBuilder<T> less(Property property, long value) {
        nativeLess(handle, property.getId(), value);
        return this;
    }

    public QueryBuilder<T> greater(Property property, long value) {
        nativeGreater(handle, property.getId(), value);
        return this;
    }

    public QueryBuilder<T> between(Property property, long value1, long value2) {
        nativeBetween(handle, property.getId(), value1, value2);
        return this;
    }

    // FIXME DbException: invalid unordered_map<K, T> key
    public QueryBuilder<T> in(Property property, long[] values) {
        nativeIn(handle, property.getId(), values);
        return this;
    }

    public QueryBuilder<T> in(Property property, int[] values) {
        nativeIn(handle, property.getId(), values);
        return this;
    }

    public QueryBuilder<T> equal(Property property, String value) {
        nativeEqual(handle, property.getId(), value, false);
        return this;
    }

    public QueryBuilder<T> notEqual(Property property, String value) {
        nativeNotEqual(handle, property.getId(), value, false);
        return this;
    }

    public QueryBuilder<T> contains(Property property, String value) {
        nativeContains(handle, property.getId(), value, false);
        return this;
    }

    public QueryBuilder<T> startsWith(Property property, String value) {
        nativeStartsWith(handle, property.getId(), value, false);
        return this;
    }

    public QueryBuilder<T> endsWith(Property property, String value) {
        nativeEndsWith(handle, property.getId(), value, false);
        return this;
    }

    public QueryBuilder<T> equal(Property property, String value, StringOrder order) {
        nativeEqual(handle, property.getId(), value, order == StringOrder.CASE_SENSITIVE);
        return this;
    }

    public QueryBuilder<T> notEqual(Property property, String value, StringOrder order) {
        nativeNotEqual(handle, property.getId(), value, order == StringOrder.CASE_SENSITIVE);
        return this;
    }

    public QueryBuilder<T> contains(Property property, String value, StringOrder order) {
        nativeContains(handle, property.getId(), value, order == StringOrder.CASE_SENSITIVE);
        return this;
    }

    public QueryBuilder<T> startsWith(Property property, String value, StringOrder order) {
        nativeStartsWith(handle, property.getId(), value, order == StringOrder.CASE_SENSITIVE);
        return this;
    }

    public QueryBuilder<T> endsWith(Property property, String value, StringOrder order) {
        nativeEndsWith(handle, property.getId(), value, order == StringOrder.CASE_SENSITIVE);
        return this;
    }

    public QueryBuilder<T> less(Property property, double value) {
        nativeLess(handle, property.getId(), value);
        return this;
    }

    public QueryBuilder<T> greater(Property property, double value) {
        nativeGreater(handle, property.getId(), value);
        return this;
    }

    public QueryBuilder<T> between(Property property, double value1, double value2) {
        nativeBetween(handle, property.getId(), value1, value2);
        return this;
    }

}
