package io.objectbox.converter;

import io.objectbox.flatbuffers.ArrayReadWriteBuf;
import io.objectbox.flatbuffers.FlexBuffers;
import io.objectbox.flatbuffers.FlexBuffersBuilder;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Converts a String map entity property to a byte array database value using FlexBuffers.
 */
public class StringMapConverter implements PropertyConverter<Map<String, String>, byte[]> {
    @Override
    public byte[] convertToDatabaseValue(Map<String, String> map) {
        if (map == null) return null;

        FlexBuffersBuilder builder = new FlexBuffersBuilder(
                new ArrayReadWriteBuf(512),
                FlexBuffersBuilder.BUILDER_FLAG_SHARE_KEYS_AND_STRINGS
        );
        int mapStart = builder.startMap();

        for (Entry<String, String> entry : map.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                throw new IllegalArgumentException("Map keys or values must not be null");
            }
            builder.putString(entry.getKey(), entry.getValue());
        }

        builder.endMap(null, mapStart);
        ByteBuffer buffer = builder.finish();

        byte[] out = new byte[buffer.limit()];
        buffer.get(out);
        return out;
    }

    @Override
    public Map<String, String> convertToEntityProperty(byte[] databaseValue) {
        if (databaseValue == null) return null;

        FlexBuffers.Map map = FlexBuffers.getRoot(new ArrayReadWriteBuf(databaseValue, databaseValue.length)).asMap();

        // As recommended by docs, iterate keys and values vectors in parallel to avoid binary search of key vector.
        int entryCount = map.size();
        FlexBuffers.KeyVector keys = map.keys();
        FlexBuffers.Vector values = map.values();
        Map<String, String> resultMap = new HashMap<>(entryCount);
        for (int i = 0; i < entryCount; i++) {
            String key = keys.get(i).toString();
            String value = values.get(i).asString();
            resultMap.put(key, value);
        }

        return resultMap;
    }
}
