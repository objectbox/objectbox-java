package io.objectbox.converter;

public class LongFlexMapConverter extends FlexMapConverter {
    @Override
    Object convertToKey(String keyValue) {
        return Long.valueOf(keyValue);
    }
}
