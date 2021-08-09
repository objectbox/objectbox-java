package io.objectbox.converter;

import io.objectbox.flatbuffers.ArrayReadWriteBuf;
import io.objectbox.flatbuffers.FlexBuffers;
import io.objectbox.flatbuffers.FlexBuffersBuilder;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Converts a String map entity property to a byte array database value using FlexBuffers.
 */
public class StringMapConverter implements PropertyConverter<Map<String, String>, byte[]> {

    private static final AtomicReference<FlexBuffersBuilder> cachedBuilder = new AtomicReference<>();

    @Override
    public byte[] convertToDatabaseValue(Map<String, String> map) {
        if (map == null) return null;

        FlexBuffersBuilder builder = cachedBuilder.getAndSet(null);
        if (builder == null) {
            builder = new FlexBuffersBuilder(
                    new ArrayReadWriteBuf(512),
                    FlexBuffersBuilder.BUILDER_FLAG_SHARE_KEYS_AND_STRINGS
            );
        }
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

        // Cache if builder does not consume too much memory
        if (buffer.limit() <= 256 * 1024) {
            builder.clear();
            cachedBuilder.getAndSet(builder);
        }

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
        // Note: avoid HashMap re-hashing by choosing large enough initial capacity.
        // From docs: If the initial capacity is greater than the maximum number of entries divided by the load factor,
        // no rehash operations will ever occur.
        // So set initial capacity based on default load factor 0.75 accordingly.
        Map<String, String> resultMap = new HashMap<>((int) (entryCount / 0.75 + 1));
        for (int i = 0; i < entryCount; i++) {
            String key = keys.get(i).toString();
            String value = values.get(i).asString();
            resultMap.put(key, value);
        }

        return resultMap;
    }
}
