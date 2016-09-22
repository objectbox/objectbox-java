package io.objectbox;

public interface Properties {
    Property[] getAllProperties();
    Property getIdProperty();
    String getDbName();
}
