package io.objectbox.converter;

import io.objectbox.flatbuffers.FlexBuffers;

/**
 * Used to automatically convert {@code Map<String, Long>}.
 */
public class StringLongMapConverter extends StringFlexMapConverter {
    @Override
    protected boolean shouldRestoreAsLong(FlexBuffers.Reference reference) {
        return true; // Restore all integers as java.lang.Long.
    }
}
