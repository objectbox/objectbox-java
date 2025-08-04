/*
 * Copyright 2021-2024 ObjectBox Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.objectbox.converter;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import io.objectbox.flatbuffers.ArrayReadWriteBuf;
import io.objectbox.flatbuffers.FlexBuffers;
import io.objectbox.flatbuffers.FlexBuffersBuilder;

/**
 * Converts between {@link Object} properties and byte arrays using FlexBuffers.
 * <p>
 * Types are limited to those supported by FlexBuffers, including that map keys must be {@link String}.
 * (There are subclasses available that auto-convert {@link Integer} and {@link Long} key maps,
 * see {@link #convertToKey}.)
 * <p>
 * If any item requires 64 bits for storage in the FlexBuffers Map/Vector (a large Long, a Double)
 * all integers are restored as {@link Long}, otherwise {@link Integer}.
 * So e.g. when storing only a {@link Long} value of {@code 1L}, the value restored from the
 * database will be of type {@link Integer}.
 * (There are subclasses available that always restore as {@link Long}, see {@link #shouldRestoreAsLong}.)
 * <p>
 * Values of type {@link Float} are always restored as {@link Double}.
 * Cast to {@link Float} to obtain the original value.
 */
public class FlexObjectConverter implements PropertyConverter<Object, byte[]> {

    private static final AtomicReference<FlexBuffersBuilder> cachedBuilder = new AtomicReference<>();

    @Override
    public byte[] convertToDatabaseValue(Object value) {
        if (value == null) return null;

        FlexBuffersBuilder builder = cachedBuilder.getAndSet(null);
        if (builder == null) {
            // Note: BUILDER_FLAG_SHARE_KEYS_AND_STRINGS is as fast as no flags for small maps/strings
            // and faster for larger maps/strings. BUILDER_FLAG_SHARE_STRINGS is always slower.
            builder = new FlexBuffersBuilder(
                    new ArrayReadWriteBuf(512),
                    FlexBuffersBuilder.BUILDER_FLAG_SHARE_KEYS_AND_STRINGS
            );
        }

        addValue(builder, value);

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

    private void addValue(FlexBuffersBuilder builder, Object value) {
        if (value instanceof Map) {
            //noinspection unchecked
            addMap(builder, null, (Map<Object, Object>) value);
        } else if (value instanceof List) {
            //noinspection unchecked
            addVector(builder, null, (List<Object>) value);
        } else if (value instanceof String) {
            builder.putString((String) value);
        } else if (value instanceof Boolean) {
            builder.putBoolean((Boolean) value);
        } else if (value instanceof Byte) {
            // Will always be restored as Integer.
            builder.putInt(((Byte) value).intValue());
        } else if (value instanceof Short) {
            // Will always be restored as Integer.
            builder.putInt(((Short) value).intValue());
        } else if (value instanceof Integer) {
            builder.putInt((Integer) value);
        } else if (value instanceof Long) {
            builder.putInt((Long) value);
        } else if (value instanceof Float) {
            builder.putFloat((Float) value);
        } else if (value instanceof Double) {
            builder.putFloat((Double) value);
        } else if (value instanceof byte[]) {
            builder.putBlob((byte[]) value);
        } else {
            throw new IllegalArgumentException(
                    "Values of this type are not supported: " + value.getClass().getSimpleName());
        }
    }

    /**
     * Checks Java map key is of the expected type, otherwise throws.
     */
    protected void checkMapKeyType(Object rawKey) {
        if (!(rawKey instanceof String)) {
            throw new IllegalArgumentException("Map keys must be String");
        }
    }

    private void addMap(FlexBuffersBuilder builder, String mapKey, Map<Object, Object> map) {
        int mapStart = builder.startMap();

        for (Map.Entry<Object, Object> entry : map.entrySet()) {
            Object rawKey = entry.getKey();
            Object value = entry.getValue();
            if (rawKey == null) {
                throw new IllegalArgumentException("Map keys must not be null");
            }
            checkMapKeyType(rawKey);
            String key = rawKey.toString();
            if (value == null) {
                builder.putNull(key);
            } else if (value instanceof Map) {
                //noinspection unchecked
                addMap(builder, key, (Map<Object, Object>) value);
            } else if (value instanceof List) {
                //noinspection unchecked
                addVector(builder, key, (List<Object>) value);
            } else if (value instanceof String) {
                builder.putString(key, (String) value);
            } else if (value instanceof Boolean) {
                builder.putBoolean(key, (Boolean) value);
            } else if (value instanceof Byte) {
                // Will always be restored as Integer.
                builder.putInt(key, ((Byte) value).intValue());
            } else if (value instanceof Short) {
                // Will always be restored as Integer.
                builder.putInt(key, ((Short) value).intValue());
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
            if (item == null) {
                builder.putNull();
            } else if (item instanceof Map) {
                //noinspection unchecked
                addMap(builder, null, (Map<Object, Object>) item);
            } else if (item instanceof List) {
                //noinspection unchecked
                addVector(builder, null, (List<Object>) item);
            } else if (item instanceof String) {
                builder.putString((String) item);
            } else if (item instanceof Boolean) {
                builder.putBoolean((Boolean) item);
            } else if (item instanceof Byte) {
                // Will always be restored as Integer.
                builder.putInt(((Byte) item).intValue());
            } else if (item instanceof Short) {
                // Will always be restored as Integer.
                builder.putInt(((Short) item).intValue());
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
    public Object convertToEntityProperty(byte[] databaseValue) {
        if (databaseValue == null) return null;

        FlexBuffers.Reference value = FlexBuffers.getRoot(new ArrayReadWriteBuf(databaseValue, databaseValue.length));
        if (value.isNull()) {
            return null;
        } else if (value.isMap()) {
            return buildMap(value.asMap());
        } else if (value.isVector()) {
            return buildList(value.asVector());
        } else if (value.isString()) {
            return value.asString();
        } else if (value.isBoolean()) {
            return value.asBoolean();
        } else if (value.isInt()) {
            if (shouldRestoreAsLong(value)) {
                return value.asLong();
            } else {
                return value.asInt();
            }
        } else if (value.isFloat()) {
            // Always return as double; if original was float consumer can cast to obtain original value.
            return value.asFloat();
        } else if (value.isBlob()) {
            return value.asBlob().getBytes();
        } else {
            throw new IllegalArgumentException("FlexBuffers type is not supported: " + value.getType());
        }
    }

    /**
     * Converts a FlexBuffers string map key to the Java map key (e.g. String to Integer).
     * <p>
     * This required conversion restricts all keys (root and embedded maps) to the same type.
     */
    Object convertToKey(String keyValue) {
        return keyValue;
    }

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
            if (value.isNull()) {
                resultMap.put(key, null);
            } else if (value.isMap()) {
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
                // Always return as double; if original was float consumer can cast to obtain original value.
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
            if (item.isNull()) {
                list.add(null);
            } else if (item.isMap()) {
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
                // Always return as double; if original was float consumer can cast to obtain original value.
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
