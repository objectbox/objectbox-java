package io.objectbox.internal;

import java.io.Serializable;
import java.util.List;

import io.objectbox.annotation.apihint.Internal;

@Internal
public interface ToManyGetter<SOURCE> extends Serializable {
    <TARGET> List<TARGET> getToMany(SOURCE object);
}
