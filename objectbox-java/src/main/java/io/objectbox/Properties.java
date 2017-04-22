package io.objectbox;

import io.objectbox.annotation.apihint.Internal;
import io.objectbox.internal.CursorFactory;
import io.objectbox.internal.IdGetter;

@Internal
// TODO rename to EntityInfo (?)
public interface Properties<T> {
    String getEntityName();
    String getDbName();

    Class<T> getEntityClass();

    Property[] getAllProperties();
    Property getIdProperty();

    IdGetter<T> getIdGetter();
    CursorFactory<T> getCursorFactory();
}
