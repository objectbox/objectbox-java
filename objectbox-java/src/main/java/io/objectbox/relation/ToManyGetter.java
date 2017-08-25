package io.objectbox.relation;

import java.io.Serializable;

import io.objectbox.annotation.apihint.Internal;

@Internal
public interface ToManyGetter<SOURCE> extends Serializable {
    <TARGET> ToMany<TARGET> getToOne(SOURCE object);
}
