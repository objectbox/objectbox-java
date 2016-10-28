package io.objectbox;

import io.objectbox.annotation.apihint.Internal;

@Internal
public interface Properties {
    Property[] getAllProperties();
    Property getIdProperty();
    String getDbName();
}
