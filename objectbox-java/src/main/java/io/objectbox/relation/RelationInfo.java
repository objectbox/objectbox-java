package io.objectbox.relation;

import javax.annotation.concurrent.Immutable;

import io.objectbox.EntityInfo;
import io.objectbox.Property;
import io.objectbox.annotation.apihint.Internal;

@Internal
@Immutable
/**
 * Meta info describing a relation including source and target entity.
 */
public class RelationInfo<TARGET> {
    public final EntityInfo sourceInfo;
    public final EntityInfo<TARGET> targetInfo;
    public final Property targetIdProperty;

    public RelationInfo(EntityInfo sourceInfo, EntityInfo<TARGET> targetInfo, Property targetIdProperty) {
        this.sourceInfo = sourceInfo;
        this.targetInfo = targetInfo;
        this.targetIdProperty = targetIdProperty;
    }
}

