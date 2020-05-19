/*
 * Copyright 2017 ObjectBox Ltd. All rights reserved.
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

package io.objectbox.internal;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import io.objectbox.annotation.apihint.Internal;

@Internal
public class ReflectionCache {
    private static final ReflectionCache instance = new ReflectionCache();

    public static ReflectionCache getInstance() {
        return instance;
    }

    private final Map<Class<?>, Map<String, Field>> fields = new HashMap<>();

    @Nonnull
    public synchronized Field getField(Class<?> clazz, String name) {
        Map<String, Field> fieldsForClass = fields.get(clazz);
        if (fieldsForClass == null) {
            fieldsForClass = new HashMap<>();
            fields.put(clazz, fieldsForClass);
        }
        Field field = fieldsForClass.get(name);
        if (field == null) {
            try {
                field = clazz.getDeclaredField(name);
                field.setAccessible(true);
            } catch (NoSuchFieldException e) {
                throw new IllegalStateException(e);
            }
            fieldsForClass.put(name, field);
        }
        return field;
    }

}
