package io.objectbox.relation;

import io.objectbox.annotation.apihint.Internal;

@Internal
public interface ToOneGetter<SOURCE> {
    <TARGET> ToOne<TARGET> getToOne(SOURCE object);
}
