package io.objectbox.reactive;

public interface Transformer<FROM, TO> {
    TO transform(FROM source) throws Exception;
}
