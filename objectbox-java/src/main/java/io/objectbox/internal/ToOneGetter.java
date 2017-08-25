package io.objectbox.internal;

import java.io.Serializable;

import io.objectbox.annotation.apihint.Internal;
import io.objectbox.relation.ToOne;

@Internal
public interface ToOneGetter<SOURCE> extends Serializable {
    <TARGET> ToOne<TARGET> getToOne(SOURCE object);
}
