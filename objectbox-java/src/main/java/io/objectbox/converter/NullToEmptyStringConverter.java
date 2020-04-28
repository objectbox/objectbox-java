package io.objectbox.converter;

import javax.annotation.Nullable;

/**
 * Used as a converter if a property is annotated with {@link io.objectbox.annotation.DefaultValue @DefaultValue("")}.
 */
public class NullToEmptyStringConverter implements PropertyConverter<String, String> {

    @Override
    public String convertToDatabaseValue(String entityProperty) {
        return entityProperty;
    }

    @Override
    public String convertToEntityProperty(@Nullable String databaseValue) {
        if (databaseValue == null) {
            return "";
        }
        return databaseValue;
    }
}
