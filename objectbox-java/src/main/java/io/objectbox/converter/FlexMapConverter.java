package io.objectbox.converter;

import io.objectbox.flatbuffers.ArrayReadWriteBuf;
import io.objectbox.flatbuffers.FlexBuffers;
import io.objectbox.flatbuffers.FlexBuffersBuilder;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Converts between {@link Map} properties and byte arrays using FlexBuffers.
 * <p>
 * All keys must have the same type (see {@link #convertToKey(String)}),
 * value types are limited to those supported by FlexBuffers.
 * <p>
 * If any item requires 64 bits for storage in the FlexBuffers Map/Vector (a large Long, a Double)
 * all integers are restored as Long, otherwise Integer.
 */
public abstract class FlexMapConverter implements PropertyConverter<Map<Object, Object>, byte[]> {

    private static final AtomicReference<FlexBuffersBuilder> cachedBuilder = new AtomicReference<>();

    @Override
    public byte[] convertToDatabaseValue(Map<Object, Object> map) {
        if (map == null) return null;

        FlexBuffersBuilder builder = cachedBuilder.getAndSet(null);
        if (builder == null) {
            // Note: BUILDER_FLAG_SHARE_KEYS_AND_STRINGS is as fast as no flags for small maps/strings
            // and faster for larger maps/strings. BUILDER_FLAG_SHARE_STRINGS is always slower.
            builder = new FlexBuffersBuilder(
                    new ArrayReadWriteBuf(512),
                    FlexBuffersBuilder.BUILDER_FLAG_SHARE_KEYS_AND_STRINGS
            );
        }

        addMap(builder, null, map);

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

    private void addMap(FlexBuffersBuilder builder, String mapKey, Map<Object, Object> map) {
        int mapStart = builder.startMap();

        for (Entry<Object, Object> entry : map.entrySet()) {
            Object value = entry.getValue();
            if (entry.getKey() == null || value == null) {
                throw new IllegalArgumentException("Map keys or values must not be null");
            }

            String key = entry.getKey().toString();
            if (value instanceof Map) {
                //noinspection unchecked
                addMap(builder, key, (Map<Object, Object>) value);
            } else if (value instanceof List) {
                //noinspection unchecked
                addVector(builder, key, (List<Object>) value);
            } else if (value instanceof String) {
                builder.putString(key, (String) value);
            } else if (value instanceof Boolean) {
                builder.putBoolean(key, (Boolean) value);
            } else if (value instanceof Integer) {
                builder.putInt(key, (Integer) value);
            } else if (value instanceof Long) {
                builder.putInt(key, (Long) value);
            } else if (value instanceof Float) {
                builder.putFloat(key, (Float) value);
            } else if (value instanceof Double) {
                builder.putFloat(key, (Double) value);
            } else if (value instanceof byte[]) {
                builder.putBlob(key, (byte[]) value);
            } else {
                throw new IllegalArgumentException(
                        "Map values of this type are not supported: " + value.getClass().getSimpleName());
            }
        }

        builder.endMap(mapKey, mapStart);
    }

    private void addVector(FlexBuffersBuilder builder, String vectorKey, List<Object> list) {
        int vectorStart = builder.startVector();

        for (Object item : list) {
            if (item instanceof Map) {
                //noinspection unchecked
                addMap(builder, null, (Map<Object, Object>) item);
            } else if (item instanceof List) {
                //noinspection unchecked
                addVector(builder, null, (List<Object>) item);
            } else if (item instanceof String) {
                builder.putString((String) item);
            } else if (item instanceof Boolean) {
                builder.putBoolean((Boolean) item);
            } else if (item instanceof Integer) {
                builder.putInt((Integer) item);
            } else if (item instanceof Long) {
                builder.putInt((Long) item);
            } else if (item instanceof Float) {
                builder.putFloat((Float) item);
            } else if (item instanceof Double) {
                builder.putFloat((Double) item);
            } else if (item instanceof byte[]) {
                builder.putBlob((byte[]) item);
            } else {
                throw new IllegalArgumentException(
                        "List values of this type are not supported: " + item.getClass().getSimpleName());
            }
        }

        builder.endVector(vectorKey, vectorStart, false, false);
    }

    @Override
    public Map<Object, Object> convertToEntityProperty(byte[] databaseValue) {
        if (databaseValue == null) return null;

        FlexBuffers.Map map = FlexBuffers.getRoot(new ArrayReadWriteBuf(databaseValue, databaseValue.length)).asMap();

        return buildMap(map);
    }

    /**
     * Converts a FlexBuffers string map key to the Java map key (e.g. String to Integer).
     * <p>
     * This required conversion restricts all keys (root and embedded maps) to the same type.
     */
    abstract Object convertToKey(String keyValue);

    /**
     * Returns true if the width in bytes stored in the private parentWidth field of FlexBuffers.Reference is 8.
     * Note: FlexBuffers stores all items in a map/vector using the size of the widest item. However,
     * an item's size is only as wide as needed, e.g. a 64-bit integer (Java Long, 8 bytes) will be
     * reduced to 1 byte if it does not exceed its value range.
     */
    protected boolean shouldRestoreAsLong(FlexBuffers.Reference reference) {
        try {
            Field parentWidthF = reference.getClass().getDeclaredField("parentWidth");
            parentWidthF.setAccessible(true);
            return (int) parentWidthF.get(reference) == 8;
        } catch (Exception e) {
            // If thrown, it is likely the FlexBuffers API has changed and the above should be updated.
            throw new RuntimeException("FlexMapConverter could not determine FlexBuffers integer bit width.", e);
        }
    }

    private Map<Object, Object> buildMap(FlexBuffers.Map map) {
        // As recommended by docs, iterate keys and values vectors in parallel to avoid binary search of key vector.
        int entryCount = map.size();
        FlexBuffers.KeyVector keys = map.keys();
        FlexBuffers.Vector values = map.values();
        // Note: avoid HashMap re-hashing by choosing large enough initial capacity.
        // From docs: If the initial capacity is greater than the maximum number of entries divided by the load factor,
        // no rehash operations will ever occur.
        // So set initial capacity based on default load factor 0.75 accordingly.
        Map<Object, Object> resultMap = new HashMap<>((int) (entryCount / 0.75 + 1));
        for (int i = 0; i < entryCount; i++) {
            String rawKey = keys.get(i).toString();
            Object key = convertToKey(rawKey);
            FlexBuffers.Reference value = values.get(i);
            if (value.isMap()) {
                resultMap.put(key, buildMap(value.asMap()));
            } else if (value.isVector()) {
                resultMap.put(key, buildList(value.asVector()));
            } else if (value.isString()) {
                resultMap.put(key, value.asString());
            } else if (value.isBoolean()) {
                resultMap.put(key, value.asBoolean());
            } else if (value.isInt()) {
                if (shouldRestoreAsLong(value)) {
                    resultMap.put(key, value.asLong());
                } else {
                    resultMap.put(key, value.asInt());
                }
            } else if (value.isFloat()) {
                // Always return as double; if original was float casting will give original value.
                resultMap.put(key, value.asFloat());
            } else if (value.isBlob()) {
                resultMap.put(key, value.asBlob().getBytes());
            } else {
                throw new IllegalArgumentException(
                        "Map values of this type are not supported: " + value.getClass().getSimpleName());
            }
        }

        return resultMap;
    }

    private List<Object> buildList(FlexBuffers.Vector vector) {
        int itemCount = vector.size();
        List<Object> list = new ArrayList<>(itemCount);

        // FlexBuffers uses the byte width of the biggest item to size all items, so only need to check the first.
        Boolean shouldRestoreAsLong = null;

        for (int i = 0; i < itemCount; i++) {
            FlexBuffers.Reference item = vector.get(i);
            if (item.isMap()) {
                list.add(buildMap(item.asMap()));
            } else if (item.isVector()) {
                list.add(buildList(item.asVector()));
            } else if (item.isString()) {
                list.add(item.asString());
            } else if (item.isBoolean()) {
                list.add(item.asBoolean());
            } else if (item.isInt()) {
                if (shouldRestoreAsLong == null) {
                    shouldRestoreAsLong = shouldRestoreAsLong(item);
                }
                if (shouldRestoreAsLong) {
                    list.add(item.asLong());
                } else {
                    list.add(item.asInt());
                }
            } else if (item.isFloat()) {
                // Always return as double; if original was float casting will give original value.
                list.add(item.asFloat());
            } else if (item.isBlob()) {
                list.add(item.asBlob().getBytes());
            } else {
                throw new IllegalArgumentException(
                        "List values of this type are not supported: " + item.getClass().getSimpleName());
            }
        }

        return list;
    }

}
