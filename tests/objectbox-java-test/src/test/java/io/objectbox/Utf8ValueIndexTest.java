package io.objectbox;

import io.objectbox.annotation.IndexType;

/**
 * Same as {@link Utf8Test}, but with index on simpleString.
 */
public class Utf8ValueIndexTest extends Utf8Test {

    @Override
    protected BoxStore createBoxStore() {
        return createBoxStore(IndexType.VALUE);
    }

}
