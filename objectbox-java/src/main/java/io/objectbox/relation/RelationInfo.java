package io.objectbox.relation;

import javax.annotation.Nullable;

import io.objectbox.Property;
import io.objectbox.annotation.apihint.Internal;
import io.objectbox.internal.IdGetter;

@Internal
public class RelationInfo<TARGET> {
    public final Class sourceClass;
    public final @Nullable Property targetIdProperty;
    public final Class<TARGET> targetClass;
    public final IdGetter<TARGET> targetIdGetter;

    public RelationInfo(Class sourceClass, @Nullable Property targetIdProperty, Class<TARGET> targetClass,
                        IdGetter<TARGET> targetIdGetter) {
        this.sourceClass = sourceClass;
        this.targetIdProperty = targetIdProperty;
        this.targetClass = targetClass;
        this.targetIdGetter = targetIdGetter;
    }
}

