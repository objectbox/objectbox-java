/*
 * Copyright 2017-2025 ObjectBox Ltd.
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

import javax.annotation.Nullable;

import io.objectbox.Box;
import io.objectbox.EntityInfo;
import io.objectbox.Property;
import io.objectbox.annotation.apihint.Internal;
import io.objectbox.exception.DbException;
import io.objectbox.relation.RelationInfo;

/**
 * Builds a {@link Query Query} using conditions which can then be used to return a list of matching Objects.
 * <p>
 * A simple example:
 *
 * <pre>
 * userBox.query()
 *     .equal(User_.firstName, "Joe", StringOrder.CASE_SENSITIVE)
 *     .order(User_.lastName)
 *     .build()
 *     .find()
 * </pre>
 *
 * <p>
 * To add a condition use the appropriate method, for example {@link #equal(Property, String, StringOrder)} or
 * {@link #isNull(Property)}. To order results use {@link #order(Property)} and its related methods.
 * <p>
 * Use {@link #build()} to create a {@link Query} object, which is used to actually get the results.
 * <p>
 * Note: by default Query returns full Objects. To return only values or an aggregate value for a single Property,
 * use {@link Query#property(Property)}.
 * <p>
 * See the <a href="https://docs.objectbox.io/queries">Queries documentation</a> for details.
 *
 * @param <T> Entity class for which the Query is built.
 */
@SuppressWarnings({"WeakerAccess", "UnusedReturnValue", "unused"})
public class QueryBuilder<T> {

    public enum StringOrder {
        /**
         * Ignores case of ASCII characters when matching results,
         * e.g. the condition "= example" matches both "Example" and "example".
         * <p>
         * Note: To utilize an index on a property use {@link #CASE_SENSITIVE} instead.
         */
        CASE_INSENSITIVE,

        /**
         * Checks case of ASCII characters when matching results,
         * e.g. the condition "= example" only matches "example", but not "Example".
         * <p>
         * Use this if the property has an {@link io.objectbox.annotation.Index @Index}
         * to dramatically increase the speed of looking-up results.
         */
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

    /**
     * Holds on to last condition. May be a property condition or a combined condition.
     */
    private long lastCondition;
    /**
     * Holds on to last property condition to use with {@link #parameterAlias(String)}
     */
    private long lastPropertyCondition;
    private Operator combineNextWith = Operator.NONE;

    @Nullable
    private List<EagerRelation<T, ?>> eagerRelations;

    @Nullable
    private QueryFilter<T> filter;

    @Nullable
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

    private native long nativeRelationCount(long handle, long storeHandle, int relationOwnerEntityId, int propertyId,
                                            int relationCount);

    // ------------------------------ (Not)Null------------------------------

    private native long nativeNull(long handle, int propertyId);

    private native long nativeNotNull(long handle, int propertyId);

    // ------------------------------ Integers ------------------------------

    private native long nativeEqual(long handle, int propertyId, long value);

    private native long nativeNotEqual(long handle, int propertyId, long value);

    private native long nativeLess(long handle, int propertyId, long value, boolean withEqual);

    private native long nativeGreater(long handle, int propertyId, long value, boolean withEqual);

    private native long nativeBetween(long handle, int propertyId, long value1, long value2);

    private native long nativeIn(long handle, int propertyId, int[] values, boolean negate);

    private native long nativeIn(long handle, int propertyId, long[] values, boolean negate);

    private native long nativeEqualKeyValue(long handle, int propertyId, String key, long value);

    private native long nativeGreaterKeyValue(long handle, int propertyId, String key, long value);

    private native long nativeGreaterEqualsKeyValue(long handle, int propertyId, String key, long value);

    private native long nativeLessKeyValue(long handle, int propertyId, String key, long value);

    private native long nativeLessEqualsKeyValue(long handle, int propertyId, String key, long value);

    // ------------------------------ Strings ------------------------------

    private native long nativeEqual(long handle, int propertyId, String value, boolean caseSensitive);

    private native long nativeNotEqual(long handle, int propertyId, String value, boolean caseSensitive);

    private native long nativeContains(long handle, int propertyId, String value, boolean caseSensitive);

    private native long nativeContainsElement(long handle, int propertyId, String value, boolean caseSensitive);

    private native long nativeEqualKeyValue(long handle, int propertyId, String key, String value, boolean caseSensitive);

    private native long nativeGreaterKeyValue(long handle, int propertyId, String key, String value, boolean caseSensitive);

    private native long nativeGreaterEqualsKeyValue(long handle, int propertyId, String key, String value, boolean caseSensitive);

    private native long nativeLessKeyValue(long handle, int propertyId, String key, String value, boolean caseSensitive);

    private native long nativeLessEqualsKeyValue(long handle, int propertyId, String key, String value, boolean caseSensitive);

    private native long nativeStartsWith(long handle, int propertyId, String value, boolean caseSensitive);

    private native long nativeEndsWith(long handle, int propertyId, String value, boolean caseSensitive);

    private native long nativeLess(long handle, int propertyId, String value, boolean caseSensitive, boolean withEqual);

    private native long nativeGreater(long handle, int propertyId, String value, boolean caseSensitive, boolean withEqual);

    private native long nativeIn(long handle, int propertyId, String[] value, boolean caseSensitive);

    // ------------------------------ FPs ------------------------------

    private native long nativeLess(long handle, int propertyId, double value, boolean withEqual);

    private native long nativeGreater(long handle, int propertyId, double value, boolean withEqual);

    private native long nativeBetween(long handle, int propertyId, double value1, double value2);

    private native long nativeNearestNeighborsF32(long handle, int propertyId, float[] queryVector, int maxResultCount);

    private native long nativeEqualKeyValue(long handle, int propertyId, String key, double value);

    private native long nativeGreaterKeyValue(long handle, int propertyId, String key, double value);

    private native long nativeGreaterEqualsKeyValue(long handle, int propertyId, String key, double value);

    private native long nativeLessKeyValue(long handle, int propertyId, String key, double value);

    private native long nativeLessEqualsKeyValue(long handle, int propertyId, String key, double value);

    // ------------------------------ Bytes ------------------------------

    private native long nativeEqual(long handle, int propertyId, byte[] value);

    private native long nativeLess(long handle, int propertyId, byte[] value, boolean withEqual);

    private native long nativeGreater(long handle, int propertyId, byte[] value, boolean withEqual);

    @Internal
    public QueryBuilder(Box<T> box, long storeHandle, String entityName) {
        this.box = box;
        this.storeHandle = storeHandle;
        handle = nativeCreate(storeHandle, entityName);
        if (handle == 0) throw new DbException("Could not create native query builder");
        isSubQuery = false;
    }

    private QueryBuilder(long storeHandle, long subQueryBuilderHandle) {
        this.box = null;
        this.storeHandle = storeHandle;
        handle = subQueryBuilderHandle;
        isSubQuery = true;
    }

    /**
     * Typically {@link #build()} is called on this which calls {@link #close()} and avoids expensive finalization here.
     * <p>
     * If {@link #build()} is not called, make sure to explicitly call {@link #close()}.
     */
    @SuppressWarnings("deprecation") // finalize()
    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

    /**
     * Close this query builder and free used resources.
     * <p>
     * This is not required when calling {@link #build()}.
     */
    // Not implementing (Auto)Closeable as QueryBuilder is typically closed due to build() getting called.
    public synchronized void close() {
        if (handle != 0) {
            // Closeable recommendation: mark as "closed" before nativeDestroy could throw.
            long handleCopy = handle;
            handle = 0;
            if (!isSubQuery) {
                nativeDestroy(handleCopy);
            }
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
        if (queryHandle == 0) throw new DbException("Could not create native query");
        Query<T> query = new Query<>(box, queryHandle, eagerRelations, filter, comparator);
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
     * Applies the given query conditions and returns the builder for further customization, such as result order.
     * Build the condition using the properties from your entity underscore classes.
     * <p>
     * An example with a nested OR condition:
     * <pre>
     * # Java
     * builder.apply(User_.name.equal("Jane")
     *         .and(User_.age.less(12)
     *                 .or(User_.status.equal("child"))));
     *
     * # Kotlin
     * builder.apply(User_.name.equal("Jane")
     *         and (User_.age.less(12)
     *         or User_.status.equal("child")))
     * </pre>
     * Use {@link Box#query(QueryCondition)} as a shortcut for this method.
     */
    public QueryBuilder<T> apply(QueryCondition<T> queryCondition) {
        ((QueryConditionImpl<T>) queryCondition).apply(this);
        return this;
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
        checkNoOperatorPending();
        nativeOrder(handle, property.getId(), flags);
        return this;
    }

    public QueryBuilder<T> sort(Comparator<T> comparator) {
        this.comparator = comparator;
        return this;
    }


    /**
     * <b>Note:</b> New code should use the {@link Box#query(QueryCondition) new query API}. Existing code can continue
     * to use this, there are currently no plans to remove the old query API.
     * <p>
     * Assigns the given alias to the previous condition.
     *
     * @param alias The string alias for use with setParameter(s) methods.
     */
    public QueryBuilder<T> parameterAlias(String alias) {
        verifyHandle();
        if (lastPropertyCondition == 0) {
            throw new IllegalStateException("No previous condition. Before you can assign an alias, you must first have a condition.");
        }
        nativeSetParameterAlias(lastPropertyCondition, alias);
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
        EntityInfo<?> relationOwner = backlink ? relationInfo.targetInfo : relationInfo.sourceInfo;
        return link(relationInfo, relationOwner, relationInfo.targetInfo, backlink);
    }

    private <TARGET> QueryBuilder<TARGET> link(RelationInfo<?, ?> relationInfo, EntityInfo<?> relationOwner,
                                               EntityInfo<?> target, boolean backlink) {
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
     * This prepares the given relation objects to be preloaded (cached) avoiding further get operations from the database.
     *
     * @param relationInfo The relation as found in the generated meta info class ("EntityName_") of class T.
     * @param more         Supply further relations to be eagerly loaded.
     */
    @SuppressWarnings("rawtypes")
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
    @SuppressWarnings({"rawtypes", "unchecked"})
    public QueryBuilder<T> eager(int limit, RelationInfo relationInfo, @Nullable RelationInfo... more) {
        verifyNotSubQuery();
        if (eagerRelations == null) {
            eagerRelations = new ArrayList<>();
        }
        eagerRelations.add(new EagerRelation<>(limit, relationInfo));
        if (more != null) {
            for (RelationInfo info : more) {
                eagerRelations.add(new EagerRelation<>(limit, info));
            }
        }
        return this;
    }

    /**
     * Sets a filter that executes on primary query results (returned from the db core) on a Java level.
     * For efficiency reasons, you should always prefer primary criteria like {@link #equal(Property, long)} if
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
        verifyHandle(); // Not using handle, but throw for consistency with other methods.
        if (lastCondition == 0) {
            throw new IllegalStateException("No previous condition. Use operators like and() and or() only between two conditions.");
        }
        checkNoOperatorPending();
        combineNextWith = operator;
    }

    private void checkNoOperatorPending() {
        if (combineNextWith != Operator.NONE) {
            throw new IllegalStateException(
                    "Another operator is pending. Use operators like and() and or() only between two conditions.");
        }
    }

    private void checkCombineCondition(long currentCondition) {
        if (combineNextWith != Operator.NONE) {
            boolean combineUsingOr = combineNextWith == Operator.OR;
            lastCondition = nativeCombine(handle, lastCondition, currentCondition, combineUsingOr);
            combineNextWith = Operator.NONE;
        } else {
            lastCondition = currentCondition;
        }
        lastPropertyCondition = currentCondition;
    }

    @Internal
    long internalGetLastCondition() {
        return lastCondition;
    }

    @Internal
    void internalAnd(long leftCondition, long rightCondition) {
        lastCondition = nativeCombine(handle, leftCondition, rightCondition, false);
    }

    @Internal
    void internalOr(long leftCondition, long rightCondition) {
        lastCondition = nativeCombine(handle, leftCondition, rightCondition, true);
    }

    /**
     * <b>Note:</b> New code should use the {@link Box#query(QueryCondition) new query API}. Existing code can continue
     * to use this, there are currently no plans to remove the old query API.
     */
    public QueryBuilder<T> isNull(Property<T> property) {
        verifyHandle();
        checkCombineCondition(nativeNull(handle, property.getId()));
        return this;
    }

    /**
     * <b>Note:</b> New code should use the {@link Box#query(QueryCondition) new query API}. Existing code can continue
     * to use this, there are currently no plans to remove the old query API.
     */
    public QueryBuilder<T> notNull(Property<T> property) {
        verifyHandle();
        checkCombineCondition(nativeNotNull(handle, property.getId()));
        return this;
    }

    /**
     * <b>Note:</b> New code should use the {@link Box#query(QueryCondition) new query API}. Existing code can continue
     * to use this, there are currently no plans to remove the old query API.
     */
    public QueryBuilder<T> relationCount(RelationInfo<T, ?> relationInfo, int relationCount) {
        verifyHandle();
        checkCombineCondition(nativeRelationCount(handle, storeHandle, relationInfo.targetInfo.getEntityId(),
                relationInfo.targetIdProperty.id, relationCount));
        return this;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //                                              Integers
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * <b>Note:</b> New code should use the {@link Box#query(QueryCondition) new query API}. Existing code can continue
     * to use this, there are currently no plans to remove the old query API.
     */
    public QueryBuilder<T> equal(Property<T> property, long value) {
        verifyHandle();
        checkCombineCondition(nativeEqual(handle, property.getId(), value));
        return this;
    }

    /**
     * <b>Note:</b> New code should use the {@link Box#query(QueryCondition) new query API}. Existing code can continue
     * to use this, there are currently no plans to remove the old query API.
     */
    public QueryBuilder<T> notEqual(Property<T> property, long value) {
        verifyHandle();
        checkCombineCondition(nativeNotEqual(handle, property.getId(), value));
        return this;
    }

    /**
     * <b>Note:</b> New code should use the {@link Box#query(QueryCondition) new query API}. Existing code can continue
     * to use this, there are currently no plans to remove the old query API.
     */
    public QueryBuilder<T> less(Property<T> property, long value) {
        verifyHandle();
        checkCombineCondition(nativeLess(handle, property.getId(), value, false));
        return this;
    }

    /**
     * <b>Note:</b> New code should use the {@link Box#query(QueryCondition) new query API}. Existing code can continue
     * to use this, there are currently no plans to remove the old query API.
     */
    public QueryBuilder<T> lessOrEqual(Property<T> property, long value) {
        verifyHandle();
        checkCombineCondition(nativeLess(handle, property.getId(), value, true));
        return this;
    }

    /**
     * <b>Note:</b> New code should use the {@link Box#query(QueryCondition) new query API}. Existing code can continue
     * to use this, there are currently no plans to remove the old query API.
     */
    public QueryBuilder<T> greater(Property<T> property, long value) {
        verifyHandle();
        checkCombineCondition(nativeGreater(handle, property.getId(), value, false));
        return this;
    }

    /**
     * <b>Note:</b> New code should use the {@link Box#query(QueryCondition) new query API}. Existing code can continue
     * to use this, there are currently no plans to remove the old query API.
     */
    public QueryBuilder<T> greaterOrEqual(Property<T> property, long value) {
        verifyHandle();
        checkCombineCondition(nativeGreater(handle, property.getId(), value, true));
        return this;
    }

    /**
     * <b>Note:</b> New code should use the {@link Box#query(QueryCondition) new query API}. Existing code can continue
     * to use this, there are currently no plans to remove the old query API.
     * <p>
     * Finds objects with property value between and including the first and second value.
     */
    public QueryBuilder<T> between(Property<T> property, long value1, long value2) {
        verifyHandle();
        checkCombineCondition(nativeBetween(handle, property.getId(), value1, value2));
        return this;
    }

    // FIXME DbException: invalid unordered_map<K, T> key

    /**
     * <b>Note:</b> New code should use the {@link Box#query(QueryCondition) new query API}. Existing code can continue
     * to use this, there are currently no plans to remove the old query API.
     */
    public QueryBuilder<T> in(Property<T> property, long[] values) {
        verifyHandle();
        checkCombineCondition(nativeIn(handle, property.getId(), values, false));
        return this;
    }

    /**
     * <b>Note:</b> New code should use the {@link Box#query(QueryCondition) new query API}. Existing code can continue
     * to use this, there are currently no plans to remove the old query API.
     */
    public QueryBuilder<T> notIn(Property<T> property, long[] values) {
        verifyHandle();
        checkCombineCondition(nativeIn(handle, property.getId(), values, true));
        return this;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Integers -> int[]
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * <b>Note:</b> New code should use the {@link Box#query(QueryCondition) new query API}. Existing code can continue
     * to use this, there are currently no plans to remove the old query API.
     */
    public QueryBuilder<T> in(Property<T> property, int[] values) {
        verifyHandle();
        checkCombineCondition(nativeIn(handle, property.getId(), values, false));
        return this;
    }

    /**
     * <b>Note:</b> New code should use the {@link Box#query(QueryCondition) new query API}. Existing code can continue
     * to use this, there are currently no plans to remove the old query API.
     */
    public QueryBuilder<T> notIn(Property<T> property, int[] values) {
        verifyHandle();
        checkCombineCondition(nativeIn(handle, property.getId(), values, true));
        return this;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Integers -> boolean
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * <b>Note:</b> New code should use the {@link Box#query(QueryCondition) new query API}. Existing code can continue
     * to use this, there are currently no plans to remove the old query API.
     */
    public QueryBuilder<T> equal(Property<T> property, boolean value) {
        verifyHandle();
        checkCombineCondition(nativeEqual(handle, property.getId(), value ? 1 : 0));
        return this;
    }

    /**
     * <b>Note:</b> New code should use the {@link Box#query(QueryCondition) new query API}. Existing code can continue
     * to use this, there are currently no plans to remove the old query API.
     */
    public QueryBuilder<T> notEqual(Property<T> property, boolean value) {
        verifyHandle();
        checkCombineCondition(nativeNotEqual(handle, property.getId(), value ? 1 : 0));
        return this;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Integers -> Date
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * <b>Note:</b> New code should use the {@link Box#query(QueryCondition) new query API}. Existing code can continue
     * to use this, there are currently no plans to remove the old query API.
     *
     * @throws NullPointerException if given value is null. Use {@link #isNull(Property)} instead.
     */
    public QueryBuilder<T> equal(Property<T> property, Date value) {
        verifyHandle();
        checkCombineCondition(nativeEqual(handle, property.getId(), value.getTime()));
        return this;
    }

    /**
     * <b>Note:</b> New code should use the {@link Box#query(QueryCondition) new query API}. Existing code can continue
     * to use this, there are currently no plans to remove the old query API.
     *
     * @throws NullPointerException if given value is null. Use {@link #isNull(Property)} instead.
     */
    public QueryBuilder<T> notEqual(Property<T> property, Date value) {
        verifyHandle();
        checkCombineCondition(nativeNotEqual(handle, property.getId(), value.getTime()));
        return this;
    }

    /**
     * <b>Note:</b> New code should use the {@link Box#query(QueryCondition) new query API}. Existing code can continue
     * to use this, there are currently no plans to remove the old query API.
     *
     * @throws NullPointerException if given value is null. Use {@link #isNull(Property)} instead.
     */
    public QueryBuilder<T> less(Property<T> property, Date value) {
        verifyHandle();
        checkCombineCondition(nativeLess(handle, property.getId(), value.getTime(), false));
        return this;
    }

    /**
     * <b>Note:</b> New code should use the {@link Box#query(QueryCondition) new query API}. Existing code can continue
     * to use this, there are currently no plans to remove the old query API.
     *
     * @throws NullPointerException if given value is null. Use {@link #isNull(Property)} instead.
     */
    public QueryBuilder<T> lessOrEqual(Property<T> property, Date value) {
        verifyHandle();
        checkCombineCondition(nativeLess(handle, property.getId(), value.getTime(), true));
        return this;
    }

    /**
     * <b>Note:</b> New code should use the {@link Box#query(QueryCondition) new query API}. Existing code can continue
     * to use this, there are currently no plans to remove the old query API.
     *
     * @throws NullPointerException if given value is null. Use {@link #isNull(Property)} instead.
     */
    public QueryBuilder<T> greater(Property<T> property, Date value) {
        verifyHandle();
        checkCombineCondition(nativeGreater(handle, property.getId(), value.getTime(), false));
        return this;
    }

    /**
     * <b>Note:</b> New code should use the {@link Box#query(QueryCondition) new query API}. Existing code can continue
     * to use this, there are currently no plans to remove the old query API.
     *
     * @throws NullPointerException if given value is null. Use {@link #isNull(Property)} instead.
     */
    public QueryBuilder<T> greaterOrEqual(Property<T> property, Date value) {
        verifyHandle();
        checkCombineCondition(nativeGreater(handle, property.getId(), value.getTime(), true));
        return this;
    }

    /**
     * <b>Note:</b> New code should use the {@link Box#query(QueryCondition) new query API}. Existing code can continue
     * to use this, there are currently no plans to remove the old query API.
     * <p>
     * Finds objects with property value between and including the first and second value.
     *
     * @throws NullPointerException if one of the given values is null.
     */
    public QueryBuilder<T> between(Property<T> property, Date value1, Date value2) {
        verifyHandle();
        checkCombineCondition(nativeBetween(handle, property.getId(), value1.getTime(), value2.getTime()));
        return this;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //                                              String
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * <b>Note:</b> New code should use the {@link Box#query(QueryCondition) new query API}. Existing code can continue
     * to use this, there are currently no plans to remove the old query API.
     * <p>
     * Creates an "equal ('=')" condition for this property.
     */
    public QueryBuilder<T> equal(Property<T> property, String value, StringOrder order) {
        verifyHandle();
        checkCombineCondition(nativeEqual(handle, property.getId(), value, order == StringOrder.CASE_SENSITIVE));
        return this;
    }

    /**
     * <b>Note:</b> New code should use the {@link Box#query(QueryCondition) new query API}. Existing code can continue
     * to use this, there are currently no plans to remove the old query API.
     * <p>
     * Creates a "not equal ('&lt;&gt;')" condition for this property.
     */
    public QueryBuilder<T> notEqual(Property<T> property, String value, StringOrder order) {
        verifyHandle();
        checkCombineCondition(nativeNotEqual(handle, property.getId(), value, order == StringOrder.CASE_SENSITIVE));
        return this;
    }

    /**
     * <b>Note:</b> New code should use the {@link Box#query(QueryCondition) new query API}. Existing code can continue
     * to use this, there are currently no plans to remove the old query API.
     * <p>
     * Creates a contains condition.
     * <p>
     * Note: for a String array property, use {@link #containsElement} instead.
     */
    public QueryBuilder<T> contains(Property<T> property, String value, StringOrder order) {
        if (String[].class == property.type) {
            throw new UnsupportedOperationException("For String[] only containsElement() is supported at this time.");
        }
        verifyHandle();
        checkCombineCondition(nativeContains(handle, property.getId(), value, order == StringOrder.CASE_SENSITIVE));
        return this;
    }

    /**
     * <b>Note:</b> New code should use the {@link Box#query(QueryCondition) new query API}. Existing code can continue
     * to use this, there are currently no plans to remove the old query API.
     * <p>
     * For a String array, list or String-key map property, matches if at least one element equals the given value.
     */
    public QueryBuilder<T> containsElement(Property<T> property, String value, StringOrder order) {
        verifyHandle();
        checkCombineCondition(nativeContainsElement(handle, property.getId(), value, order == StringOrder.CASE_SENSITIVE));
        return this;
    }

    /**
     * @deprecated Use {@link Property#equalKeyValue(String, String, StringOrder)} with the
     * {@link Box#query(QueryCondition) new query API} instead.
     */
    @Deprecated
    public QueryBuilder<T> containsKeyValue(Property<T> property, String key, String value, StringOrder order) {
        verifyHandle();
        checkCombineCondition(nativeEqualKeyValue(handle, property.getId(), key, value, order == StringOrder.CASE_SENSITIVE));
        return this;
    }

    /**
     * Note: Use {@link Property#equalKeyValue(String, String, StringOrder)} with the
     * {@link Box#query(QueryCondition) new query API} instead.
     */
    public QueryBuilder<T> equalKeyValue(Property<T> property, String key, String value, StringOrder order) {
        verifyHandle();
        checkCombineCondition(nativeEqualKeyValue(handle, property.getId(), key, value, order == StringOrder.CASE_SENSITIVE));
        return this;
    }

    /**
     * Note: Use {@link Property#lessKeyValue(String, String, StringOrder)} with the
     * {@link Box#query(QueryCondition) new query API} instead.
     */
    public QueryBuilder<T> lessKeyValue(Property<T> property, String key, String value, StringOrder order) {
        verifyHandle();
        checkCombineCondition(nativeLessKeyValue(handle, property.getId(), key, value, order == StringOrder.CASE_SENSITIVE));
        return this;
    }

    /**
     * Note: Use {@link Property#lessOrEqualKeyValue(String, String, StringOrder)} with the
     * {@link Box#query(QueryCondition) new query API} instead.
     */
    public QueryBuilder<T> lessOrEqualKeyValue(Property<T> property, String key, String value, StringOrder order) {
        verifyHandle();
        checkCombineCondition(nativeLessEqualsKeyValue(handle, property.getId(), key, value, order == StringOrder.CASE_SENSITIVE));
        return this;
    }

    /**
     * Note: Use {@link Property#greaterKeyValue(String, String, StringOrder)} with the
     * {@link Box#query(QueryCondition) new query API} instead.
     */
    public QueryBuilder<T> greaterKeyValue(Property<T> property, String key, String value, StringOrder order) {
        verifyHandle();
        checkCombineCondition(nativeGreaterKeyValue(handle, property.getId(), key, value, order == StringOrder.CASE_SENSITIVE));
        return this;
    }

    /**
     * Note: Use {@link Property#greaterOrEqualKeyValue(String, String, StringOrder)} with the
     * {@link Box#query(QueryCondition) new query API} instead.
     */
    public QueryBuilder<T> greaterOrEqualKeyValue(Property<T> property, String key, String value, StringOrder order) {
        verifyHandle();
        checkCombineCondition(nativeGreaterEqualsKeyValue(handle, property.getId(), key, value, order == StringOrder.CASE_SENSITIVE));
        return this;
    }

    /**
     * Note: Use {@link Property#equalKeyValue(String, long)} with the
     * {@link Box#query(QueryCondition) new query API} instead.
     */
    public QueryBuilder<T> equalKeyValue(Property<T> property, String key, long value) {
        verifyHandle();
        checkCombineCondition(nativeEqualKeyValue(handle, property.getId(), key, value));
        return this;
    }

    /**
     * Note: Use {@link Property#lessKeyValue(String, long)} with the
     * {@link Box#query(QueryCondition) new query API} instead.
     */
    public QueryBuilder<T> lessKeyValue(Property<T> property, String key, long value) {
        verifyHandle();
        checkCombineCondition(nativeLessKeyValue(handle, property.getId(), key, value));
        return this;
    }

    /**
     * Note: Use {@link Property#lessOrEqualKeyValue(String, long)} with the
     * {@link Box#query(QueryCondition) new query API} instead.
     */
    public QueryBuilder<T> lessOrEqualKeyValue(Property<T> property, String key, long value) {
        verifyHandle();
        checkCombineCondition(nativeLessEqualsKeyValue(handle, property.getId(), key, value));
        return this;
    }

    /**
     * Note: Use {@link Property#greaterOrEqualKeyValue(String, long)} (String, String, StringOrder)} with the
     * {@link Box#query(QueryCondition) new query API} instead.
     */
    public QueryBuilder<T> greaterKeyValue(Property<T> property, String key, long value) {
        verifyHandle();
        checkCombineCondition(nativeGreaterKeyValue(handle, property.getId(), key, value));
        return this;
    }

    /**
     * Note: Use {@link Property#greaterOrEqualKeyValue(String, long)} with the
     * {@link Box#query(QueryCondition) new query API} instead.
     */
    public QueryBuilder<T> greaterOrEqualKeyValue(Property<T> property, String key, long value) {
        verifyHandle();
        checkCombineCondition(nativeGreaterEqualsKeyValue(handle, property.getId(), key, value));
        return this;
    }

    /**
     * Note: Use {@link Property#equalKeyValue(String, double)} with the
     * {@link Box#query(QueryCondition) new query API} instead.
     */
    public QueryBuilder<T> equalKeyValue(Property<T> property, String key, double value) {
        verifyHandle();
        checkCombineCondition(nativeEqualKeyValue(handle, property.getId(), key, value));
        return this;
    }

    /**
     * Note: Use {@link Property#lessKeyValue(String, double)} with the
     * {@link Box#query(QueryCondition) new query API} instead.
     */
    public QueryBuilder<T> lessKeyValue(Property<T> property, String key, double value) {
        verifyHandle();
        checkCombineCondition(nativeLessKeyValue(handle, property.getId(), key, value));
        return this;
    }

    /**
     * Note: Use {@link Property#lessOrEqualKeyValue(String, double)} with the
     * {@link Box#query(QueryCondition) new query API} instead.
     */
    public QueryBuilder<T> lessOrEqualKeyValue(Property<T> property, String key, double value) {
        verifyHandle();
        checkCombineCondition(nativeLessEqualsKeyValue(handle, property.getId(), key, value));
        return this;
    }

    /**
     * Note: Use {@link Property#greaterKeyValue(String, double)} with the
     * {@link Box#query(QueryCondition) new query API} instead.
     */
    public QueryBuilder<T> greaterKeyValue(Property<T> property, String key, double value) {
        verifyHandle();
        checkCombineCondition(nativeGreaterKeyValue(handle, property.getId(), key, value));
        return this;
    }

    /**
     * Note: Use {@link Property#greaterOrEqualKeyValue(String, double)} with the
     * {@link Box#query(QueryCondition) new query API} instead.
     */
    public QueryBuilder<T> greaterOrEqualKeyValue(Property<T> property, String key, double value) {
        verifyHandle();
        checkCombineCondition(nativeGreaterEqualsKeyValue(handle, property.getId(), key, value));
        return this;
    }

    /**
     * <b>Note:</b> New code should use the {@link Box#query(QueryCondition) new query API}. Existing code can continue
     * to use this, there are currently no plans to remove the old query API.
     */
    public QueryBuilder<T> startsWith(Property<T> property, String value, StringOrder order) {
        verifyHandle();
        checkCombineCondition(nativeStartsWith(handle, property.getId(), value, order == StringOrder.CASE_SENSITIVE));
        return this;
    }

    /**
     * <b>Note:</b> New code should use the {@link Box#query(QueryCondition) new query API}. Existing code can continue
     * to use this, there are currently no plans to remove the old query API.
     */
    public QueryBuilder<T> endsWith(Property<T> property, String value, StringOrder order) {
        verifyHandle();
        checkCombineCondition(nativeEndsWith(handle, property.getId(), value, order == StringOrder.CASE_SENSITIVE));
        return this;
    }

    /**
     * <b>Note:</b> New code should use the {@link Box#query(QueryCondition) new query API}. Existing code can continue
     * to use this, there are currently no plans to remove the old query API.
     */
    public QueryBuilder<T> less(Property<T> property, String value, StringOrder order) {
        verifyHandle();
        checkCombineCondition(nativeLess(handle, property.getId(), value, order == StringOrder.CASE_SENSITIVE, false));
        return this;
    }

    /**
     * <b>Note:</b> New code should use the {@link Box#query(QueryCondition) new query API}. Existing code can continue
     * to use this, there are currently no plans to remove the old query API.
     */
    public QueryBuilder<T> lessOrEqual(Property<T> property, String value, StringOrder order) {
        verifyHandle();
        checkCombineCondition(nativeLess(handle, property.getId(), value, order == StringOrder.CASE_SENSITIVE, true));
        return this;
    }

    /**
     * <b>Note:</b> New code should use the {@link Box#query(QueryCondition) new query API}. Existing code can continue
     * to use this, there are currently no plans to remove the old query API.
     */
    public QueryBuilder<T> greater(Property<T> property, String value, StringOrder order) {
        verifyHandle();
        checkCombineCondition(nativeGreater(handle, property.getId(), value, order == StringOrder.CASE_SENSITIVE, false));
        return this;
    }

    /**
     * <b>Note:</b> New code should use the {@link Box#query(QueryCondition) new query API}. Existing code can continue
     * to use this, there are currently no plans to remove the old query API.
     */
    public QueryBuilder<T> greaterOrEqual(Property<T> property, String value, StringOrder order) {
        verifyHandle();
        checkCombineCondition(nativeGreater(handle, property.getId(), value, order == StringOrder.CASE_SENSITIVE, true));
        return this;
    }

    /**
     * <b>Note:</b> New code should use the {@link Box#query(QueryCondition) new query API}. Existing code can continue
     * to use this, there are currently no plans to remove the old query API.
     */
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
     * <b>Note:</b> New code should use the {@link Box#query(QueryCondition) new query API}. Existing code can continue
     * to use this, there are currently no plans to remove the old query API.
     * <p>
     * Floating point equality is non-trivial; this is just a convenience for
     * {@link #between(Property, double, double)} with parameters(property, value - tolerance, value + tolerance).
     * When using {@link Query#setParameters(Property, double, double)},
     * consider that the params are the lower and upper bounds.
     */
    public QueryBuilder<T> equal(Property<T> property, double value, double tolerance) {
        return between(property, value - tolerance, value + tolerance);
    }

    /**
     * <b>Note:</b> New code should use the {@link Box#query(QueryCondition) new query API}. Existing code can continue
     * to use this, there are currently no plans to remove the old query API.
     */
    public QueryBuilder<T> less(Property<T> property, double value) {
        verifyHandle();
        checkCombineCondition(nativeLess(handle, property.getId(), value, false));
        return this;
    }

    /**
     * <b>Note:</b> New code should use the {@link Box#query(QueryCondition) new query API}. Existing code can continue
     * to use this, there are currently no plans to remove the old query API.
     */
    public QueryBuilder<T> lessOrEqual(Property<T> property, double value) {
        verifyHandle();
        checkCombineCondition(nativeLess(handle, property.getId(), value, true));
        return this;
    }

    /**
     * <b>Note:</b> New code should use the {@link Box#query(QueryCondition) new query API}. Existing code can continue
     * to use this, there are currently no plans to remove the old query API.
     */
    public QueryBuilder<T> greater(Property<T> property, double value) {
        verifyHandle();
        checkCombineCondition(nativeGreater(handle, property.getId(), value, false));
        return this;
    }

    /**
     * <b>Note:</b> New code should use the {@link Box#query(QueryCondition) new query API}. Existing code can continue
     * to use this, there are currently no plans to remove the old query API.
     */
    public QueryBuilder<T> greaterOrEqual(Property<T> property, double value) {
        verifyHandle();
        checkCombineCondition(nativeGreater(handle, property.getId(), value, true));
        return this;
    }

    /**
     * <b>Note:</b> New code should use the {@link Box#query(QueryCondition) new query API}. Existing code can continue
     * to use this, there are currently no plans to remove the old query API.
     * <p>
     * Finds objects with property value between and including the first and second value.
     */
    public QueryBuilder<T> between(Property<T> property, double value1, double value2) {
        verifyHandle();
        checkCombineCondition(nativeBetween(handle, property.getId(), value1, value2));
        return this;
    }

    /**
     * <b>Note:</b> New code should use the {@link Box#query(QueryCondition) new query API}. Existing code can continue
     * to use this, there are currently no plans to remove the old query API.
     */
    public QueryBuilder<T> nearestNeighbors(Property<T> property, float[] queryVector, int maxResultCount) {
        verifyHandle();
        checkCombineCondition(nativeNearestNeighborsF32(handle, property.getId(), queryVector, maxResultCount));
        return this;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //                                                 Bytes
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * <b>Note:</b> New code should use the {@link Box#query(QueryCondition) new query API}. Existing code can continue
     * to use this, there are currently no plans to remove the old query API.
     */
    public QueryBuilder<T> equal(Property<T> property, byte[] value) {
        verifyHandle();
        checkCombineCondition(nativeEqual(handle, property.getId(), value));
        return this;
    }

    /**
     * <b>Note:</b> New code should use the {@link Box#query(QueryCondition) new query API}. Existing code can continue
     * to use this, there are currently no plans to remove the old query API.
     */
    public QueryBuilder<T> less(Property<T> property, byte[] value) {
        verifyHandle();
        checkCombineCondition(nativeLess(handle, property.getId(), value, false));
        return this;
    }

    /**
     * <b>Note:</b> New code should use the {@link Box#query(QueryCondition) new query API}. Existing code can continue
     * to use this, there are currently no plans to remove the old query API.
     */
    public QueryBuilder<T> lessOrEqual(Property<T> property, byte[] value) {
        verifyHandle();
        checkCombineCondition(nativeLess(handle, property.getId(), value, true));
        return this;
    }

    /**
     * <b>Note:</b> New code should use the {@link Box#query(QueryCondition) new query API}. Existing code can continue
     * to use this, there are currently no plans to remove the old query API.
     */
    public QueryBuilder<T> greater(Property<T> property, byte[] value) {
        verifyHandle();
        checkCombineCondition(nativeGreater(handle, property.getId(), value, false));
        return this;
    }

    /**
     * <b>Note:</b> New code should use the {@link Box#query(QueryCondition) new query API}. Existing code can continue
     * to use this, there are currently no plans to remove the old query API.
     */
    public QueryBuilder<T> greaterOrEqual(Property<T> property, byte[] value) {
        verifyHandle();
        checkCombineCondition(nativeGreater(handle, property.getId(), value, true));
        return this;
    }

}
