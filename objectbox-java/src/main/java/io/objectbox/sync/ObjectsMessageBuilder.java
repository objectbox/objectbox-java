package io.objectbox.sync;

/**
 * @see SyncClient#startObjectsMessage
 */
public interface ObjectsMessageBuilder {

    ObjectsMessageBuilder addString(long optionalId, String value);

    ObjectsMessageBuilder addBytes(long optionalId, byte[] value, boolean isFlatBuffers);

    /**
     * Sends the message, returns true if successful.
     */
    boolean send();
}
