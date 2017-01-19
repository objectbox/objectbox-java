package io.objectbox.relation;

import java.lang.reflect.Field;

import io.objectbox.Box;
import io.objectbox.BoxStore;
import io.objectbox.annotation.apihint.Experimental;
import io.objectbox.exception.DbDetachedException;
import io.objectbox.exception.DbException;

/**
 * Sketching a replacement for length generation of to-one getters...
 */
@Experimental
public abstract class ToOne<SOURCE, TARGET> {
    protected final SOURCE entity;
    protected final Class<TARGET> targetClass;

    protected volatile Box<TARGET> targetBox;

    /**
     * Resolved target entity is cached
     */
    protected TARGET target;

    public ToOne(SOURCE entity, Class<TARGET> targetClass) {
        this.entity = entity;
        this.targetClass = targetClass;
    }

    private volatile long resolvedKey;

    public TARGET getTarget() {
        long key = getToOneKey();
        if (resolvedKey != key) {
            if (targetBox == null) {
                final BoxStore boxStore = getBoxStore();
                if (boxStore == null) {
                    throw new DbDetachedException();
                }
                targetBox = boxStore.boxFor(targetClass);
            }
            TARGET targetNew = targetBox.get(key);
            synchronized (this) {
                target = targetNew;
                resolvedKey = key;
            }
        }
        return target;
    }

    public boolean isResolved() {
        return resolvedKey != getToOneKey();
    }

    public boolean isResolvedAndNotNull() {
        return resolvedKey != 0 && resolvedKey != getToOneKey();
    }

    public boolean isNull() {
        return getToOneKey() == 0;
    }

    /**
     * Clears the target.
     */
    public synchronized void clear() {
        resolvedKey = 0;
        target = null;
    }

    private BoxStore getBoxStore() {
        Field boxStoreField;
        try {
            boxStoreField = entity.getClass().getDeclaredField("__boxStore");
            boxStoreField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new DbException("Can not resolve to-one relationship, entity class has no __boxStore field: "
                    + entity.getClass());
        }
        try {
            BoxStore boxStore = (BoxStore) boxStoreField.get(entity);
            if (boxStore == null) {
                throw new DbDetachedException("Cannot resolve relation for detached entities");
            }
            return boxStore;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /** Implemented by generated ToOne sub classes. Alternative: reflection. */
    public abstract long getToOneKey();
}
