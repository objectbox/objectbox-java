/*
 * Copyright 2017-2018 ObjectBox Ltd. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.objectbox.query;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import io.objectbox.Box;
import io.objectbox.EntityInfo;
import io.objectbox.Property;
import io.objectbox.annotation.apihint.Experimental;
import io.objectbox.annotation.apihint.Internal;
import io.objectbox.relation.RelationInfo;

/**
 * With QueryBuilder you define custom queries returning matching entities. Using the methods of this class you can
 * select (filter) results for specific data (for example #{@link #equal(Property, String)} and
 * {@link #isNull(Property)}) and select an sort order for the resulting list (see {@link #order(Property)} and its
 * overloads).
 * <p>
 * Use {@link #build()} to conclude your query definitions and to get a {@link Query} object, which is used to actually
 * get results.
 * <p>
 * Note: Currently you can only query for complete entities. Returning individual property values or aggregates are
 * currently not available. Keep in mind that ObjectBox is very fast and the overhead to create an entity is very low.
 *
 * @param <T> Entity class associated with this query builder.
 */
@SuppressWarnings({"WeakerAccess", "UnusedReturnValue", "unused"})
@Experimental
public class QueryBuilder<T> {

    public enum StringOrder {
        /** The default: case insensitive ASCII characters */
        CASE_INSENSITIVE,

        /** Case matters ('a' != 'A'). */
        CASE_SENSITIVE
    }

    enum Operator {
        NONE, AND, OR
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

    private final long storeHandle;

    private long handle;

    private boolean hasOrder;

    private long lastCondition;
    private Operator combineNextWith = Operator.NONE;

    private List<EagerRelation> eagerRelations;

    private QueryFilter<T> filter;

    private Comparator<T> comparator;

    private final boolean isSubQuery;

    private native long nativeCreate(long storeHandle, String entityName);

    private native void nativeDestroy(long handle);

    private native long nativeBuild(long handle);

    private native long nativeLink(long handle, long storeHandle, int relationOwnerEntityId, int targetEntityId,
                                   int propertyId, int relationId, boolean backlink);

    private native void nativeOrder(long handle, int propertyId, int flags);

    private native long nativeCombine(long handle, long condition1, long condition2, boolean combineUsingOr);

    private native void nativeSetParameterAlias(long conditionHandle, String alias);

    // ------------------------------ (Not)Null------------------------------

    private native long nativeNull(long handle, int propertyId);

    private native long nativeNotNull(long handle, int propertyId);

    // ------------------------------ Integers ------------------------------

    private native long nativeEqual(long handle, int propertyId, long value);

    private native long nativeNotEqual(long handle, int propertyId, long value);

    private native long nativeLess(long handle, int propertyId, long value);

    private native long nativeGreater(long handle, int propertyId, long value);

    private native long nativeBetween(long handle, int propertyId, long value1, long value2);

    private native long nativeIn(long handle, int propertyId, int[] values, boolean negate);

    private native long nativeIn(long handle, int propertyId, long[] values, boolean negate);

    // ------------------------------ Strings ------------------------------

    private native long nativeEqual(long handle, int propertyId, String value, boolean caseSensitive);

    private native long nativeNotEqual(long handle, int propertyId, String value, boolean caseSensitive);

    private native long nativeContains(long handle, int propertyId, String value, boolean caseSensitive);

    private native long nativeStartsWith(long handle, int propertyId, String value, boolean caseSensitive);

    private native long nativeEndsWith(long handle, int propertyId, String value, boolean caseSensitive);

    private native long nativeLess(long handle, int propertyId, String value, boolean caseSensitive);

    private native long nativeGreater(long handle, int propertyId, String value, boolean caseSensitive);

    private native long nativeIn(long handle, int propertyId, String[] value, boolean caseSensitive);

    // ------------------------------ FPs ------------------------------

    private native long nativeLess(long handle, int propertyId, double value);

    private native long nativeGreater(long handle, int propertyId, double value);

    private native long nativeBetween(long handle, int propertyId, double value1, double value2);

    // ------------------------------ Bytes ------------------------------

    private native long nativeEqual(long handle, int propertyId, byte[] value);

    private native long nativeLess(long handle, int propertyId, byte[] value);

    private native long nativeGreater(long handle, int propertyId, byte[] value);

    @Internal
    public QueryBuilder(Box<T> box, long storeHandle, String entityName) {
        this.box = box;
        this.storeHandle = storeHandle;
        handle = nativeCreate(storeHandle, entityName);
        isSubQuery = false;
    }

    private QueryBuilder(long storeHandle, long subQueryBuilderHandle) {
        this.box = null;
        this.storeHandle = storeHandle;
        handle = subQueryBuilderHandle;
        isSubQuery = true;
    }

    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

    public synchronized void close() {
        if (handle != 0) {
            if (!isSubQuery) {
                nativeDestroy(handle);
            }
            handle = 0;
        }
    }

    /**
     * Builds the query and closes this QueryBuilder.
     */
    public Query<T> build() {
        verifyNotSubQuery();
        verifyHandle();
        if (combineNextWith != Operator.NONE) {
            throw new IllegalStateException("Incomplete logic condition. Use or()/and() between two conditions only.");
        }
        long queryHandle = nativeBuild(handle);
        Query<T> query = new Query<>(box, queryHandle, hasOrder, eagerRelations, filter, comparator);
        close();
        return query;
    }

    private void verifyNotSubQuery() {
        if (isSubQuery) {
            throw new IllegalStateException("This call is not supported on sub query builders (links)");
        }
    }

    private void verifyHandle() {
        if (handle == 0) {
            throw new IllegalStateException("This QueryBuilder has already been closed. Please use a new instance.");
        }
    }

    /**
     * Specifies given property to be used for sorting.
     * Shorthand for {@link #order(Property, int)} with flags equal to 0.
     *
     * @see #order(Property, int)
     * @see #orderDesc(Property)
     */
    public QueryBuilder<T> order(Property<T> property) {
        return order(property, 0);
    }

    /**
     * Specifies given property in descending order to be used for sorting.
     * Shorthand for {@link #order(Property, int)} with flags equal to {@link #DESCENDING}.
     *
     * @see #order(Property, int)
     * @see #order(Property)
     */
    public QueryBuilder<T> orderDesc(Property<T> property) {
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
    public QueryBuilder<T> order(Property<T> property, int flags) {
        verifyNotSubQuery();
        verifyHandle();
        if (combineNextWith != Operator.NONE) {
            throw new IllegalStateException(
                    "An operator is pending. Use operators like and() and or() only between two conditions.");
        }
        nativeOrder(handle, property.getId(), flags);
        hasOrder = true;
        return this;
    }

    public QueryBuilder<T> sort(Comparator<T> comparator) {
        this.comparator = comparator;
        return this;
    }


    /**
     * Asigns the given alias to the previous condition.
     *
     * @param alias The string alias for use with setParameter(s) methods.
     */
    public QueryBuilder<T> parameterAlias(String alias) {
        verifyHandle();
        if (lastCondition == 0) {
            throw new IllegalStateException("No previous condition. Before you can assign an alias, you must first have a condition.");
        }
        nativeSetParameterAlias(lastCondition, alias);
        return this;
    }

    /**
     * Creates a link to another entity, for which you also can describe conditions using the returned builder.
     * <p>
     * Note: in relational databases you would use a "join" for this.
     *
     * @param relationInfo Relation meta info (generated)
     * @param <TARGET>     The target entity. For parent/tree like relations, it can be the same type.
     * @return A builder to define query conditions at the target entity side.
     */
    public <TARGET> QueryBuilder<TARGET> link(RelationInfo<?, TARGET> relationInfo) {
        boolean backlink = relationInfo.isBacklink();
        EntityInfo relationOwner = backlink ? relationInfo.targetInfo : relationInfo.sourceInfo;
        return link(relationInfo, relationOwner, relationInfo.targetInfo, backlink);
    }

    private <TARGET> QueryBuilder<TARGET> link(RelationInfo relationInfo, EntityInfo relationOwner, EntityInfo target,
                                               boolean backlink) {
        int propertyId = relationInfo.targetIdProperty != null ? relationInfo.targetIdProperty.id : 0;
        int relationId = relationInfo.targetRelationId != 0 ? relationInfo.targetRelationId : relationInfo.relationId;
        long linkQBHandle = nativeLink(handle, storeHandle, relationOwner.getEntityId(), target.getEntityId(),
                propertyId, relationId, backlink);
        return new QueryBuilder<>(storeHandle, linkQBHandle);
    }

    /**
     * Creates a backlink (reversed link) to another entity,
     * for which you also can describe conditions using the returned builder.
     * <p>
     * Note: only use this method over {@link #link(RelationInfo)},
     * if you did not define @{@link io.objectbox.annotation.Backlink} in the entity already.
     * <p>
     * Note: in relational databases you would use a "join" for this.
     *
     * @param relationInfo Relation meta info (generated) of the original relation (reverse direction)
     * @param <TARGET>     The target entity. For parent/tree like relations, it can be the same type.
     * @return A builder to define query conditions at the target entity side.
     */
    public <TARGET> QueryBuilder<TARGET> backlink(RelationInfo<TARGET, ?> relationInfo) {
        if (relationInfo.isBacklink()) {
            throw new IllegalArgumentException("Double backlink: The relation is already a backlink, please use a regular link on the original relation instead.");
        }
        return link(relationInfo, relationInfo.sourceInfo, relationInfo.sourceInfo, true);
    }

    /**
     * Specifies relations that should be resolved eagerly.
     * This prepares the given relation objects to be preloaded (cached) avoiding further get operations from the db.
     * A common use case is prealoading all
     *
     * @param relationInfo The relation as found in the generated meta info class ("EntityName_") of class T.
     * @param more         Supply further relations to be eagerly loaded.
     */
    public QueryBuilder<T> eager(RelationInfo relationInfo, RelationInfo... more) {
        return eager(0, relationInfo, more);
    }

    /**
     * Like {@link #eager(RelationInfo, RelationInfo[])}, but limits eager loading to the given count.
     *
     * @param limit        Count of entities to be eager loaded.
     * @param relationInfo The relation as found in the generated meta info class ("EntityName_") of class T.
     * @param more         Supply further relations to be eagerly loaded.
     */
    public QueryBuilder<T> eager(int limit, RelationInfo relationInfo, RelationInfo... more) {
        verifyNotSubQuery();
        if (eagerRelations == null) {
            eagerRelations = new ArrayList<>();
        }
        eagerRelations.add(new EagerRelation(limit, relationInfo));
        if (more != null) {
            for (RelationInfo info : more) {
                eagerRelations.add(new EagerRelation(limit, info));
            }
        }
        return this;
    }

    /**
     * Sets a filter that executes on primary query results (returned from the db core) on a Java level.
     * For efficiency reasons, you should always prefer primary criteria like {@link #equal(Property, String)} if
     * possible.
     * A filter requires to instantiate full Java objects beforehand, which is less efficient.
     * <p>
     * The upside of filters is that they allow any complex operation including traversing object graphs,
     * and that filtering is executed along with the query (preferably in a background thread).
     * Use filtering wisely ;-).
     * <p>
     * Also note, that a filter may only be used along with {@link Query#find()} and
     * {@link Query#forEach(QueryConsumer)} at this point.
     * Other find methods will throw a exception and aggregate functions will silently ignore the filter.
     */
    public QueryBuilder<T> filter(QueryFilter<T> filter) {
        verifyNotSubQuery();
        if (this.filter != null) {
            throw new IllegalStateException("A filter was already defined, you can only assign one filter");
        }
        this.filter = filter;
        return this;
    }

    /**
     * Combines the previous condition with the following condition with a logical OR.
     * <p>
     * Example (querying t-shirts):
     * <pre>{@code
     * queryBuilder.equal(color, "blue").or().less(price, 30).build() // color is blue OR price < 30
     * }</pre>
     */
    public QueryBuilder<T> or() {
        combineOperator(Operator.OR);
        return this;
    }

    /**
     * And AND changes how conditions are combined using a following OR.
     * By default, all query conditions are already combined using AND.
     * Do not use this method if all your query conditions must match (AND for all, this is the default).
     * <p>
     * However, this method change the precedence with other combinations such as {@link #or()}.
     * This is best explained by example.
     * <p>
     * Example (querying t-shirts):
     * <pre>{@code
     * // Case (1): OR has precedence
     * queryBuilder.equal(color, "blue").equal(size, "XL").or().less(price, 30).build()
     *
     * // Case (2): AND has precedence
     * queryBuilder.equal(color, "blue").and().equal(size, "XL").or().less(price, 30).build()
     * }</pre>
     * <p>
     * Rule: Explicit AND / OR combination have precedence.
     * <p>
     * That's why (1) is evaluated like "must be blue and is either of size XL or costs less than 30", or more formally:
     * blue AND (size XL OR price less than 30).
     * <p>
     * Rule: Conditions are applied from left to right (in the order they are called).
     * <p>
     * That's why in (2) the AND is evaluated before the OR.
     * Thus, (2) evaluates to "either must be blue and of size XL, or costs less than 30", or, more formally:
     * (blue AND size XL) OR price less than 30.
     */
    public QueryBuilder<T> and() {
        combineOperator(Operator.AND);
        return this;
    }

    private void combineOperator(Operator operator) {
        if (lastCondition == 0) {
            throw new IllegalStateException("No previous condition. Use operators like and() and or() only between two conditions.");
        }
        if (combineNextWith != Operator.NONE) {
            throw new IllegalStateException("Another operator is pending. Use operators like and() and or() only between two conditions.");
        }
        combineNextWith = operator;
    }

    private void checkCombineCondition(long currentCondition) {
        if (combineNextWith != Operator.NONE) {
            boolean combineUsingOr = combineNextWith == Operator.OR;
            lastCondition = nativeCombine(handle, lastCondition, currentCondition, combineUsingOr);
            combineNextWith = Operator.NONE;
        } else {
            lastCondition = currentCondition;
        }
    }

    public QueryBuilder<T> isNull(Property<T> property) {
        verifyHandle();
        checkCombineCondition(nativeNull(handle, property.getId()));
        return this;
    }

    public QueryBuilder<T> notNull(Property<T> property) {
        verifyHandle();
        checkCombineCondition(nativeNotNull(handle, property.getId()));
        return this;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //                                              Integers
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public QueryBuilder<T> equal(Property<T> property, long value) {
        verifyHandle();
        checkCombineCondition(nativeEqual(handle, property.getId(), value));
        return this;
    }

    public QueryBuilder<T> equal(Property<T> property, boolean value) {
        verifyHandle();
        checkCombineCondition(nativeEqual(handle, property.getId(), value ? 1 : 0));
        return this;
    }

    /** @throws NullPointerException if given value is null. Use {@link #isNull(Property)} instead. */
    public QueryBuilder<T> equal(Property<T> property, Date value) {
        verifyHandle();
        checkCombineCondition(nativeEqual(handle, property.getId(), value.getTime()));
        return this;
    }

    public QueryBuilder<T> notEqual(Property<T> property, long value) {
        verifyHandle();
        checkCombineCondition(nativeNotEqual(handle, property.getId(), value));
        return this;
    }

    public QueryBuilder<T> notEqual(Property<T> property, boolean value) {
        verifyHandle();
        checkCombineCondition(nativeNotEqual(handle, property.getId(), value ? 1 : 0));
        return this;
    }

    /** @throws NullPointerException if given value is null. Use {@link #isNull(Property)} instead. */
    public QueryBuilder<T> notEqual(Property<T> property, Date value) {
        verifyHandle();
        checkCombineCondition(nativeNotEqual(handle, property.getId(), value.getTime()));
        return this;
    }

    public QueryBuilder<T> less(Property<T> property, long value) {
        verifyHandle();
        checkCombineCondition(nativeLess(handle, property.getId(), value));
        return this;
    }

    public QueryBuilder<T> greater(Property<T> property, long value) {
        verifyHandle();
        checkCombineCondition(nativeGreater(handle, property.getId(), value));
        return this;
    }

    public QueryBuilder<T> less(Property<T> property, Date value) {
        verifyHandle();
        checkCombineCondition(nativeLess(handle, property.getId(), value.getTime()));
        return this;
    }

    /** @throws NullPointerException if given value is null. Use {@link #isNull(Property)} instead. */
    public QueryBuilder<T> greater(Property<T> property, Date value) {
        verifyHandle();
        checkCombineCondition(nativeGreater(handle, property.getId(), value.getTime()));
        return this;
    }

    public QueryBuilder<T> between(Property<T> property, long value1, long value2) {
        verifyHandle();
        checkCombineCondition(nativeBetween(handle, property.getId(), value1, value2));
        return this;
    }

    /** @throws NullPointerException if one of the given values is null. */
    public QueryBuilder<T> between(Property<T> property, Date value1, Date value2) {
        verifyHandle();
        checkCombineCondition(nativeBetween(handle, property.getId(), value1.getTime(), value2.getTime()));
        return this;
    }

    // FIXME DbException: invalid unordered_map<K, T> key
    public QueryBuilder<T> in(Property<T> property, long[] values) {
        verifyHandle();
        checkCombineCondition(nativeIn(handle, property.getId(), values, false));
        return this;
    }

    public QueryBuilder<T> in(Property<T> property, int[] values) {
        verifyHandle();
        checkCombineCondition(nativeIn(handle, property.getId(), values, false));
        return this;
    }

    public QueryBuilder<T> notIn(Property<T> property, long[] values) {
        verifyHandle();
        checkCombineCondition(nativeIn(handle, property.getId(), values, true));
        return this;
    }

    public QueryBuilder<T> notIn(Property<T> property, int[] values) {
        verifyHandle();
        checkCombineCondition(nativeIn(handle, property.getId(), values, true));
        return this;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //                                              String
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public QueryBuilder<T> equal(Property<T> property, String value) {
        verifyHandle();
        checkCombineCondition(nativeEqual(handle, property.getId(), value, false));
        return this;
    }

    public QueryBuilder<T> equal(Property<T> property, String value, StringOrder order) {
        verifyHandle();
        checkCombineCondition(nativeEqual(handle, property.getId(), value, order == StringOrder.CASE_SENSITIVE));
        return this;
    }

    public QueryBuilder<T> notEqual(Property<T> property, String value) {
        verifyHandle();
        checkCombineCondition(nativeNotEqual(handle, property.getId(), value, false));
        return this;
    }

    public QueryBuilder<T> notEqual(Property<T> property, String value, StringOrder order) {
        verifyHandle();
        checkCombineCondition(nativeNotEqual(handle, property.getId(), value, order == StringOrder.CASE_SENSITIVE));
        return this;
    }

    public QueryBuilder<T> contains(Property<T> property, String value) {
        verifyHandle();
        checkCombineCondition(nativeContains(handle, property.getId(), value, false));
        return this;
    }

    public QueryBuilder<T> startsWith(Property<T> property, String value) {
        verifyHandle();
        checkCombineCondition(nativeStartsWith(handle, property.getId(), value, false));
        return this;
    }

    public QueryBuilder<T> endsWith(Property<T> property, String value) {
        verifyHandle();
        checkCombineCondition(nativeEndsWith(handle, property.getId(), value, false));
        return this;
    }

    public QueryBuilder<T> contains(Property<T> property, String value, StringOrder order) {
        verifyHandle();
        checkCombineCondition(nativeContains(handle, property.getId(), value, order == StringOrder.CASE_SENSITIVE));
        return this;
    }

    public QueryBuilder<T> startsWith(Property<T> property, String value, StringOrder order) {
        verifyHandle();
        checkCombineCondition(nativeStartsWith(handle, property.getId(), value, order == StringOrder.CASE_SENSITIVE));
        return this;
    }

    public QueryBuilder<T> endsWith(Property<T> property, String value, StringOrder order) {
        verifyHandle();
        checkCombineCondition(nativeEndsWith(handle, property.getId(), value, order == StringOrder.CASE_SENSITIVE));
        return this;
    }

    public QueryBuilder<T> less(Property<T> property, String value) {
        return less(property, value, StringOrder.CASE_INSENSITIVE);
    }

    public QueryBuilder<T> less(Property<T> property, String value, StringOrder order) {
        verifyHandle();
        checkCombineCondition(nativeLess(handle, property.getId(), value, order == StringOrder.CASE_SENSITIVE));
        return this;
    }

    public QueryBuilder<T> greater(Property<T> property, String value) {
        return greater(property, value, StringOrder.CASE_INSENSITIVE);
    }

    public QueryBuilder<T> greater(Property<T> property, String value, StringOrder order) {
        verifyHandle();
        checkCombineCondition(nativeGreater(handle, property.getId(), value, order == StringOrder.CASE_SENSITIVE));
        return this;
    }

    public QueryBuilder<T> in(Property<T> property, String[] values) {
        return in(property, values, StringOrder.CASE_INSENSITIVE);
    }

    public QueryBuilder<T> in(Property<T> property, String[] values, StringOrder order) {
        verifyHandle();
        checkCombineCondition(nativeIn(handle, property.getId(), values, order == StringOrder.CASE_SENSITIVE));
        return this;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //                                             Floating point
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    // Help people with floating point equality...

    /**
     * Floating point equality is non-trivial; this is just a convenience for
     * {@link #between(Property, double, double)} with parameters(property, value - tolerance, value + tolerance).
     * When using {@link Query#setParameters(Property, double, double)},
     * consider that the params are the lower and upper bounds.
     */
    public QueryBuilder<T> equal(Property<T> property, double value, double tolerance) {
        return between(property, value - tolerance, value + tolerance);
    }

    public QueryBuilder<T> less(Property<T> property, double value) {
        verifyHandle();
        checkCombineCondition(nativeLess(handle, property.getId(), value));
        return this;
    }

    public QueryBuilder<T> greater(Property<T> property, double value) {
        verifyHandle();
        checkCombineCondition(nativeGreater(handle, property.getId(), value));
        return this;
    }

    public QueryBuilder<T> between(Property<T> property, double value1, double value2) {
        verifyHandle();
        checkCombineCondition(nativeBetween(handle, property.getId(), value1, value2));
        return this;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //                                                 Bytes
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public QueryBuilder<T> equal(Property<T> property, byte[] value) {
        verifyHandle();
        checkCombineCondition(nativeEqual(handle, property.getId(), value));
        return this;
    }

    public QueryBuilder<T> less(Property<T> property, byte[] value) {
        verifyHandle();
        checkCombineCondition(nativeLess(handle, property.getId(), value));
        return this;
    }

    public QueryBuilder<T> greater(Property<T> property, byte[] value) {
        verifyHandle();
        checkCombineCondition(nativeGreater(handle, property.getId(), value));
        return this;
    }

}
