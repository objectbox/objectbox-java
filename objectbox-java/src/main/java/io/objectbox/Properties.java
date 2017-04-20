package io.objectbox;

import io.objectbox.annotation.apihint.Internal;
import io.objectbox.internal.IdGetter;

@Internal
public interface Properties {
    Property[] getAllProperties();
    Property getIdProperty();
    String getDbName();

    @Internal
    <T> IdGetter<T> getIdGetter();
}
