package io.objectbox.relation;

import java.io.Serializable;

import javax.annotation.concurrent.Immutable;

import io.objectbox.EntityInfo;
import io.objectbox.Property;
import io.objectbox.annotation.apihint.Internal;

@Internal
@Immutable
/**
 * Meta info describing a relation including source and target entity.
 */
public class RelationInfo<TARGET> implements Serializable {
    private static final long serialVersionUID = 7412962174183812632L;

    public final EntityInfo sourceInfo;
    public final EntityInfo<TARGET> targetInfo;

    /** For relations based on a target ID property (null for stand-alone relations). */
    public final Property targetIdProperty;

    /** For ToMany relations based on backlinks (null otherwise). */
    public final ToOneGetter backlinkToOneGetter;

    /** For stand-alone to-many relations (0 otherwise). */
    public final int relationId;

    /**
     * ToOne
     */
    public RelationInfo(EntityInfo sourceInfo, EntityInfo<TARGET> targetInfo, Property targetIdProperty) {
        this(sourceInfo, targetInfo, targetIdProperty, null);
    }

    public RelationInfo(EntityInfo sourceInfo, EntityInfo<TARGET> targetInfo, Property targetIdProperty,
                        ToOneGetter backlinkToOneGetter) {
        this.sourceInfo = sourceInfo;
        this.targetInfo = targetInfo;
        this.targetIdProperty = targetIdProperty;
        this.backlinkToOneGetter = backlinkToOneGetter;
        relationId = 0;
    }

    /**
     * Stand-alone to-many.
     */
    public RelationInfo(EntityInfo sourceInfo, EntityInfo<TARGET> targetInfo, int relationId) {
        this.sourceInfo = sourceInfo;
        this.targetInfo = targetInfo;
        this.relationId = relationId;
        this.targetIdProperty = null;
        this.backlinkToOneGetter = null;
    }
}

