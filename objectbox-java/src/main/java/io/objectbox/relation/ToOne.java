package io.objectbox.relation;

import java.lang.reflect.Field;

import io.objectbox.Box;
import io.objectbox.BoxStore;
import io.objectbox.Property;
import io.objectbox.annotation.apihint.Beta;
import io.objectbox.exception.DbDetachedException;
import io.objectbox.internal.ReflectionCache;

/**
 * To be used in generated to-one getters etc.
 */
@Beta
public class ToOne<SOURCE, TARGET> {
    private final SOURCE entity;
    private final Class<SOURCE> entityClass;
    private final Class<TARGET> targetClass;
    private final Property keyProperty;

    private BoxStore boxStore;
    private Box<SOURCE> entityBox;
    private volatile Box<TARGET> targetBox;
    private Field keyField;

    /**
     * Resolved target entity is cached
     */
    protected TARGET target;

    public ToOne(SOURCE entity, Property keyProperty, Class<TARGET> targetClass) {
        this.entity = entity;
        entityClass = (Class<SOURCE>) entity.getClass();
        this.targetClass = targetClass;
        this.keyProperty = keyProperty;
    }

    private volatile long resolvedKey;

    public TARGET getTarget() {
        return getTarget(getToOneId());
    }

    private TARGET getTarget(long key) {
        if (resolvedKey != key) {
            ensureBoxes();
            TARGET targetNew = targetBox.get(key);
            synchronized (this) {
                target = targetNew;
                resolvedKey = key;
            }
        }
        return target;
    }

    private void ensureBoxes() {
        // Only check the property set last
        if (targetBox == null) {
            Field boxStoreField = ReflectionCache.getInstance().getField(entity.getClass(), "__boxStore");
            try {
                boxStore = (BoxStore) boxStoreField.get(entity);
                if (boxStore == null) {
                    throw new DbDetachedException("Cannot resolve relation for detached entities");
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            entityBox = boxStore.boxFor(entityClass);
            targetBox = boxStore.boxFor(targetClass);
        }
    }

    public TARGET getCachedTarget() {
        return target;
    }

    public boolean isResolved() {
        return resolvedKey != getToOneId();
    }

    public boolean isResolvedAndNotNull() {
        return resolvedKey != 0 && resolvedKey != getToOneId();
    }

    public boolean isNull() {
        return getToOneId() == 0;
    }

    public void setTargetId(long targetId) {
        try {
            getKeyField().set(entity, targetId);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Could not update to-one ID in entity", e);
        }
    }

    void setAndUpdateTargetId(long targetId) {
        setTargetId(targetId);
        ensureBoxes();
        // TODO update on targetId in DB
        throw new UnsupportedOperationException("Not implemented yet");
    }


    /**
     * Sets the relation ID in the enclosed entity to the ID of the given target entity and puts the enclosed entity.
     * If the target entity was not put in the DB yet (its ID is 0), it will be put before to get its ID.
     */
    // TODO provide a overload with a ToMany parameter, which also get updated
    public void setAndPutTarget(final TARGET target) {
        ensureBoxes();
        long targetId = targetBox.getId(target);
        if (targetId == 0) {
            setAndPutTargetAlways(target);
        } else {
            try {
                getKeyField().set(entity, targetId);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Could not update to-one ID in entity", e);
            }
            setTargetId(targetId);
            entityBox.put(entity);
        }
    }

    /**
     * Sets the relation ID in the enclosed entity to the ID of the given target entity and puts both entities.
     */
    // TODO provide a overload with a ToMany parameter, which also get updated
    public void setAndPutTargetAlways(final TARGET target) {
        ensureBoxes();
        boxStore.runInTx(new Runnable() {
            @Override
            public void run() {
                long targetKey = targetBox.put(target);
                try {
                    getKeyField().set(entity, targetKey);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Could not update to-one ID in entity", e);
                }
                entityBox.put(entity);
            }
        });
    }

    /**
     * Clears the target.
     */
    public synchronized void clear() {
        resolvedKey = 0;
        target = null;
    }

    /** Implemented by generated ToOne sub classes. Alternative: reflection. */
    public long getToOneId() {
        Field keyField = getKeyField();
        try {
            Long key = (Long) keyField.get(entity);
            return key != null ? key : 0;
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Could not access field " + keyField);
        }
    }

    private Field getKeyField() {
        if (keyField == null) {
            keyField = ReflectionCache.getInstance().getField(entity.getClass(), keyProperty.name);
        }
        return keyField;
    }
}
