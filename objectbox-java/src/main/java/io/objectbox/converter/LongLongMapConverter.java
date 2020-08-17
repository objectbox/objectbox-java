package io.objectbox.converter;

import io.objectbox.flatbuffers.FlexBuffers;

/**
 * Used to automatically convert {@code Map<Long, Long>}.
 */
public class LongLongMapConverter extends LongFlexMapConverter {
    @Override
    protected boolean shouldRestoreAsLong(FlexBuffers.Reference reference) {
        return true; // Restore all integers as java.lang.Long.
    }
}
