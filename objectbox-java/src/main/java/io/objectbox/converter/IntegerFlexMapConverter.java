package io.objectbox.converter;

/**
 * Used to automatically convert {@code Map<Integer, V>}.
 */
public class IntegerFlexMapConverter extends FlexMapConverter {
    @Override
    Integer convertToKey(String keyValue) {
        return Integer.valueOf(keyValue);
    }
}
