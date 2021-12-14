package io.objectbox.converter;

/**
 * Used to automatically convert {@code Map&lt;Integer, V&gt;}.
 */
public class IntegerFlexMapConverter extends FlexObjectConverter {

    @Override
    protected void checkMapKeyType(Object rawKey) {
        if (!(rawKey instanceof Integer)) {
            throw new IllegalArgumentException("Map keys must be Integer");
        }
    }

    @Override
    Integer convertToKey(String keyValue) {
        return Integer.valueOf(keyValue);
    }
}
