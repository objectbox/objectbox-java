package io.objectbox.converter;

import io.objectbox.flatbuffers.FlexBuffers;

/**
 * Used to automatically convert {@code Map&lt;Long, Long&gt;}.
 * <p>
 * Unlike {@link FlexObjectConverter} always restores integer map values as {@link Long}.
 */
public class LongLongMapConverter extends LongFlexMapConverter {
    @Override
    protected boolean shouldRestoreAsLong(FlexBuffers.Reference reference) {
        return true; // Restore all integers as java.lang.Long.
    }
}
