package io.objectbox.converter;

public class StringFlexMapConverter extends FlexMapConverter {
    @Override
    Object convertToKey(String keyValue) {
        return keyValue;
    }
}
