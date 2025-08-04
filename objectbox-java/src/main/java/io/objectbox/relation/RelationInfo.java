/*
 * Copyright 2017 ObjectBox Ltd.
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

package io.objectbox.relation;

import java.io.Serializable;

import javax.annotation.concurrent.Immutable;

import io.objectbox.EntityInfo;
import io.objectbox.Property;
import io.objectbox.annotation.apihint.Internal;
import io.objectbox.internal.ToManyGetter;
import io.objectbox.internal.ToOneGetter;
import io.objectbox.query.QueryCondition;
import io.objectbox.query.RelationCountCondition;

/**
 * Meta info describing a relation including source and target entity.
 */
@Internal
@Immutable
public class RelationInfo<SOURCE, TARGET> implements Serializable {
    private static final long serialVersionUID = 7412962174183812632L;

    public final EntityInfo<SOURCE> sourceInfo;
    public final EntityInfo<TARGET> targetInfo;

    /** For relations based on a target ID property (null otherwise). */
    public final Property<?> targetIdProperty;

    /** For ToMany relations based on ToMany backlinks (0 otherwise). */
    public final int targetRelationId;

    /** Only set for ToOne relations */
    public final ToOneGetter<SOURCE, TARGET> toOneGetter;

    /** Only set for ToMany relations */
    public final ToManyGetter<SOURCE, TARGET> toManyGetter;

    /** For ToMany relations based on ToOne backlinks (null otherwise). */
    public final ToOneGetter<TARGET, SOURCE> backlinkToOneGetter;

    /** For ToMany relations based on ToMany backlinks (null otherwise). */
    public final ToManyGetter<TARGET, SOURCE> backlinkToManyGetter;

    /** For stand-alone to-many relations (0 otherwise). */
    public final int relationId;

    /**
     * ToOne
     */
    public RelationInfo(EntityInfo<SOURCE> sourceInfo, EntityInfo<TARGET> targetInfo, Property<SOURCE> targetIdProperty,
                        ToOneGetter<SOURCE, TARGET> toOneGetter) {
        this.sourceInfo = sourceInfo;
        this.targetInfo = targetInfo;
        this.targetIdProperty = targetIdProperty;
        this.toOneGetter = toOneGetter;
        this.targetRelationId = 0;
        this.backlinkToOneGetter = null;
        this.backlinkToManyGetter = null;
        this.toManyGetter = null;
        this.relationId = 0;
    }

    /**
     * ToMany as a ToOne backlink
     */
    public RelationInfo(EntityInfo<SOURCE> sourceInfo, EntityInfo<TARGET> targetInfo, ToManyGetter<SOURCE, TARGET> toManyGetter,
                        Property<TARGET> targetIdProperty, ToOneGetter<TARGET, SOURCE> backlinkToOneGetter) {
        this.sourceInfo = sourceInfo;
        this.targetInfo = targetInfo;
        this.targetIdProperty = targetIdProperty;
        this.toManyGetter = toManyGetter;
        this.backlinkToOneGetter = backlinkToOneGetter;
        this.targetRelationId = 0;
        this.toOneGetter = null;
        this.backlinkToManyGetter = null;
        this.relationId = 0;
    }

    /**
     * ToMany as a ToMany backlink
     */
    public RelationInfo(EntityInfo<SOURCE> sourceInfo, EntityInfo<TARGET> targetInfo, ToManyGetter<SOURCE, TARGET> toManyGetter,
                        ToManyGetter<TARGET, SOURCE> backlinkToManyGetter, int targetRelationId) {
        this.sourceInfo = sourceInfo;
        this.targetInfo = targetInfo;
        this.toManyGetter = toManyGetter;
        this.targetRelationId = targetRelationId;
        this.backlinkToManyGetter = backlinkToManyGetter;
        this.targetIdProperty = null;
        this.toOneGetter = null;
        this.backlinkToOneGetter = null;
        this.relationId = 0;
    }

    /**
     * Stand-alone ToMany.
     */
    public RelationInfo(EntityInfo<SOURCE> sourceInfo, EntityInfo<TARGET> targetInfo, ToManyGetter<SOURCE, TARGET> toManyGetter,
                        int relationId) {
        this.sourceInfo = sourceInfo;
        this.targetInfo = targetInfo;
        this.toManyGetter = toManyGetter;
        this.relationId = relationId;
        this.targetRelationId = 0;
        this.targetIdProperty = null;
        this.toOneGetter = null;
        this.backlinkToOneGetter = null;
        this.backlinkToManyGetter = null;
    }

    public boolean isBacklink() {
        return backlinkToManyGetter != null || backlinkToOneGetter != null;
    }

    @Override
    public String toString() {
        return "RelationInfo from " + sourceInfo.getEntityClass() + " to " + targetInfo.getEntityClass();
    }

    /**
     * Creates a condition to match objects that have {@code relationCount} related objects pointing to them.
     * <pre>
     * try (Query&lt;Customer&gt; query = customerBox
     *         .query(Customer_.orders.relationCount(2))
     *         .build()) {
     *     List&lt;Customer&gt; customersWithTwoOrders = query.find();
     * }
     * </pre>
     * {@code relationCount} may be 0 to match objects that do not have related objects.
     * It typically should be a low number.
     * <p>
     * This condition has some limitations:
     * <ul>
     *     <li>only 1:N (ToMany using @Backlink) relations are supported,</li>
     *     <li>the complexity is {@code O(n * (relationCount + 1))} and cannot be improved via indexes,</li>
     *     <li>the relation count cannot be changed with setParameter once the query is built.</li>
     * </ul>
     */
    public QueryCondition<SOURCE> relationCount(int relationCount) {
        if (targetIdProperty == null) {
            throw new IllegalStateException("The relation count condition is only supported for 1:N (ToMany using @Backlink) relations.");
        }
        return new RelationCountCondition<>(this, relationCount);
    }
}

