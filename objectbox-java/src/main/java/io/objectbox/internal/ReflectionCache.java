package io.objectbox.internal;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import io.objectbox.annotation.apihint.Internal;

@Internal
public class ReflectionCache {
    private static ReflectionCache instance = new ReflectionCache();

    public static ReflectionCache getInstance() {
        return instance;
    }

    private Map<Class, Map<String, Field>> fields = new HashMap<>();

    public synchronized Field getField(Class clazz, String name) {
        Map<String, Field> fieldsForClass = fields.get(clazz);
        if (fieldsForClass == null) {
            fieldsForClass = new HashMap<>();
            fields.put(clazz, fieldsForClass);
        }
        Field field = fieldsForClass.get(name);
        if (field == null) {
            try {
                field = clazz.getDeclaredField("__boxStore");
                field.setAccessible(true);
            } catch (NoSuchFieldException e) {
                throw new IllegalStateException(e);
            }
            fieldsForClass.put(name, field);
        }
        return field;
    }

}
