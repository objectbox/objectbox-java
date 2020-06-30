package io.objectbox.converter;

import io.objectbox.flatbuffers.ArrayReadWriteBuf;
import io.objectbox.flatbuffers.FlexBuffers;
import io.objectbox.flatbuffers.FlexBuffersBuilder;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Converts between {@link Map} properties and byte arrays using FlexBuffers.
 *
 * All keys must have the same type (see {@link #convertToKey(String)}),
 * value types are limited to those supported by FlexBuffers.
 */
public abstract class FlexMapConverter implements PropertyConverter<Map<Object, Object>, byte[]> {

    @Override
    public byte[] convertToDatabaseValue(Map<Object, Object> map) {
        if (map == null) return null;

        FlexBuffersBuilder builder = new FlexBuffersBuilder(
                new ArrayReadWriteBuf(512),
                FlexBuffersBuilder.BUILDER_FLAG_SHARE_KEYS_AND_STRINGS
        );

        addMap(builder, null, map);

        ByteBuffer buffer = builder.finish();

        byte[] out = new byte[buffer.limit()];
        buffer.get(out);
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
            // FIXME When restoring, can't know if Integer or Long.
//            } else if (value instanceof Integer) {
//                builder.putInt(key, (Integer) value);
            } else if (value instanceof Long) {
                builder.putInt(key, (Long) value);
            // FIXME When restoring, can't know if Float or Double.
//            } else if (value instanceof Float) {
//                builder.putFloat(key, (Float) value);
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
            // FIXME When restoring, can't know if Integer or Long.
//            } else if (item instanceof Integer) {
//                builder.putInt((Integer) item);
            } else if (item instanceof Long) {
                builder.putInt((Long) item);
            // FIXME When restoring, can't know if Float or Double.
//            } else if (item instanceof Float) {
//                builder.putFloat((Float) item);
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
     *
     * This required conversion restricts all keys (root and embedded maps) to the same type.
     */
    abstract Object convertToKey(String keyValue);

    private Map<Object, Object> buildMap(FlexBuffers.Map map) {
        // As recommended by docs, iterate keys and values vectors in parallel to avoid binary search of key vector.
        int entryCount = map.size();
        FlexBuffers.KeyVector keys = map.keys();
        FlexBuffers.Vector values = map.values();
        Map<Object, Object> resultMap = new HashMap<>(entryCount);
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
                // FIXME Integer or Long?
//                resultMap.put(key, value.asInt());
                resultMap.put(key, value.asLong());
            } else if (value.isFloat()) {
                // FIXME Float or Double? Reading Float as Double is destructive.
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
                // FIXME Integer or Long?
//                list.add(item.asInt());
                list.add(item.asLong());
            } else if (item.isFloat()) {
                // FIXME Float or Double? Reading Float as Double is destructive.
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
