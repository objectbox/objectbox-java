package io.objectbox.relation;

import java.io.Serializable;

import io.objectbox.annotation.apihint.Internal;

@Internal
public interface ToOneGetter<SOURCE> extends Serializable {
    <TARGET> ToOne<TARGET> getToOne(SOURCE object);
}
