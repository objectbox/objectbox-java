package io.objectbox.converter;

/**
 * Used to automatically convert {@code Map<Long, V>}.
 */
public class LongFlexMapConverter extends FlexMapConverter {
    @Override
    Object convertToKey(String keyValue) {
        return Long.valueOf(keyValue);
    }
}
