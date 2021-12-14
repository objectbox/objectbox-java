package io.objectbox.converter;

/**
 * Used to automatically convert {@code Map<Long, V>}.
 */
public class LongFlexMapConverter extends FlexObjectConverter {

    @Override
    protected void checkMapKeyType(Object rawKey) {
        if (!(rawKey instanceof Long)) {
            throw new IllegalArgumentException("Map keys must be Long");
        }
    }

    @Override
    Object convertToKey(String keyValue) {
        return Long.valueOf(keyValue);
    }
}
