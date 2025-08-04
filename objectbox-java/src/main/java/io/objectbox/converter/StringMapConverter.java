/*
 * Copyright 2020-2021 ObjectBox Ltd.
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
            // Note: BUILDER_FLAG_SHARE_KEYS_AND_STRINGS is as fast as no flags for small maps/strings
            // and faster for larger maps/strings. BUILDER_FLAG_SHARE_STRINGS is always slower.
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
