/*
 * Copyright 2017 ObjectBox Ltd. All rights reserved.
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
    public final ToOneGetter<SOURCE> toOneGetter;

    /** Only set for ToMany relations */
    public final ToManyGetter<SOURCE> toManyGetter;

    /** For ToMany relations based on ToOne backlinks (null otherwise). */
    public final ToOneGetter<TARGET> backlinkToOneGetter;

    /** For ToMany relations based on ToMany backlinks (null otherwise). */
    public final ToManyGetter<TARGET> backlinkToManyGetter;

    /** For stand-alone to-many relations (0 otherwise). */
    public final int relationId;

    /**
     * ToOne
     */
    public RelationInfo(EntityInfo<SOURCE> sourceInfo, EntityInfo<TARGET> targetInfo, Property<SOURCE> targetIdProperty,
                        ToOneGetter<SOURCE> toOneGetter) {
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
    public RelationInfo(EntityInfo<SOURCE> sourceInfo, EntityInfo<TARGET> targetInfo, ToManyGetter<SOURCE> toManyGetter,
                        Property<TARGET> targetIdProperty, ToOneGetter<TARGET> backlinkToOneGetter) {
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
    public RelationInfo(EntityInfo<SOURCE> sourceInfo, EntityInfo<TARGET> targetInfo, ToManyGetter<SOURCE> toManyGetter,
            ToManyGetter<TARGET> backlinkToManyGetter, int targetRelationId) {
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
    public RelationInfo(EntityInfo<SOURCE> sourceInfo, EntityInfo<TARGET> targetInfo, ToManyGetter<SOURCE> toManyGetter,
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
}

