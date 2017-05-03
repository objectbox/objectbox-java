package io.objectbox;

import io.objectbox.internal.CursorFactory;
import io.objectbox.internal.IdGetter;

public class SimpleEntityInfo<T> implements EntityInfo<T> {

    String entityName;
    String dbName;
    Class<T> entityClass;
    Property[] allProperties;
    Property idProperty;
    IdGetter<T> idGetter;
    CursorFactory<T> cursorFactory;

    @Override
    public String getEntityName() {
        return entityName;
    }

    public SimpleEntityInfo<T> setEntityName(String entityName) {
        this.entityName = entityName;
        return this;
    }

    @Override
    public String getDbName() {
        return dbName;
    }

    public SimpleEntityInfo<T> setDbName(String dbName) {
        this.dbName = dbName;
        return this;
    }

    public Class<T> getEntityClass() {
        return entityClass;
    }

    @Override
    public int getEntityId() {
        return 2;
    }

    public SimpleEntityInfo<T> setEntityClass(Class<T> entityClass) {
        this.entityClass = entityClass;
        return this;
    }

    @Override
    public Property[] getAllProperties() {
        return allProperties;
    }

    public SimpleEntityInfo<T> setAllProperties(Property[] allProperties) {
        this.allProperties = allProperties;
        return this;
    }

    @Override
    public Property getIdProperty() {
        return idProperty;
    }

    public SimpleEntityInfo<T> setIdProperty(Property idProperty) {
        this.idProperty = idProperty;
        return this;
    }

    @Override
    public IdGetter<T> getIdGetter() {
        return idGetter;
    }

    public SimpleEntityInfo<T> setIdGetter(IdGetter<T> idGetter) {
        this.idGetter = idGetter;
        return this;
    }

    @Override
    public CursorFactory<T> getCursorFactory() {
        return cursorFactory;
    }

    public SimpleEntityInfo<T> setCursorFactory(CursorFactory<T> cursorFactory) {
        this.cursorFactory = cursorFactory;
        return this;
    }
}
