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

    /** For ToMany relations based on backlinks (null otherwise). */
    public final ToOneGetter toOneGetter;

    public RelationInfo(EntityInfo sourceInfo, EntityInfo<TARGET> targetInfo, Property targetIdProperty) {
        this(sourceInfo, targetInfo, targetIdProperty, null);
    }

    public RelationInfo(EntityInfo sourceInfo, EntityInfo<TARGET> targetInfo, Property targetIdProperty,
                        ToOneGetter toOneGetter) {
        this.sourceInfo = sourceInfo;
        this.targetInfo = targetInfo;
        this.targetIdProperty = targetIdProperty;
        this.toOneGetter = toOneGetter;
    }
}

