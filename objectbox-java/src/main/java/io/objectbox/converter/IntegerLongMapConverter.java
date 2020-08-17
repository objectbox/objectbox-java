package io.objectbox.converter;

import io.objectbox.flatbuffers.FlexBuffers;

/**
 * Used to automatically convert {@code Map<Integer, Long>}.
 */
public class IntegerLongMapConverter extends IntegerFlexMapConverter {
    @Override
    protected boolean shouldRestoreAsLong(FlexBuffers.Reference reference) {
        return true; // Restore all integers as java.lang.Long.
    }
}
