package io.objectbox.internal;

import java.io.Serializable;

import io.objectbox.annotation.apihint.Internal;
import io.objectbox.relation.ToMany;

@Internal
public interface ToManyGetter<SOURCE> extends Serializable {
    <TARGET> ToMany<TARGET> getToMany(SOURCE object);
}
