package io.objectbox.converter;

/**
 * Used to automatically convert {@code Map<String, V>}.
 */
public class StringFlexMapConverter extends FlexMapConverter {
    @Override
    Object convertToKey(String keyValue) {
        return keyValue;
    }
}
