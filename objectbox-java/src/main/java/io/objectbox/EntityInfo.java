package io.objectbox;

import java.io.Serializable;

import io.objectbox.annotation.apihint.Internal;
import io.objectbox.internal.CursorFactory;
import io.objectbox.internal.IdGetter;

@Internal
public interface EntityInfo<T> extends Serializable {
    String getEntityName();

    String getDbName();

    Class<T> getEntityClass();

    int getEntityId();

    Property[] getAllProperties();

    Property getIdProperty();

    IdGetter<T> getIdGetter();

    CursorFactory<T> getCursorFactory();

    // TODO replace reflection: BoxGetter<T> getBoxGetter();
}
