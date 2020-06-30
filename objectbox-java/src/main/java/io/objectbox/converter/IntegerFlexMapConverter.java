package io.objectbox.converter;

public class IntegerFlexMapConverter extends FlexMapConverter {
    @Override
    Integer convertToKey(String keyValue) {
        return Integer.valueOf(keyValue);
    }
}
