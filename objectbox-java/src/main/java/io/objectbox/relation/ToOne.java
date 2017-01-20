package io.objectbox.relation;

import java.lang.reflect.Field;

import io.objectbox.Box;
import io.objectbox.BoxStore;
import io.objectbox.Property;
import io.objectbox.annotation.apihint.Beta;
import io.objectbox.annotation.apihint.Internal;
import io.objectbox.exception.DbDetachedException;
import io.objectbox.internal.ReflectionCache;

/**
 * To be used in generated to-one getters etc.
 */
@Beta
// TODO add tests
public class ToOne<SOURCE, TARGET> {
    private final SOURCE entity;
    private final Class<SOURCE> entityClass;
    private final Class<TARGET> targetClass;
    private final Property relationIdProperty;

    private BoxStore boxStore;
    private Box<SOURCE> entityBox;
    private volatile Box<TARGET> targetBox;
    private Field targetIdField;

    /**
     * Resolved target entity is cached
     */
    protected TARGET target;

    private volatile long resolvedTargetId;

    public ToOne(SOURCE entity, Property relationIdProperty, Class<TARGET> targetClass) {
        this.entity = entity;
        entityClass = (Class<SOURCE>) entity.getClass();
        this.targetClass = targetClass;
        this.relationIdProperty = relationIdProperty;
    }


    public TARGET getTarget() {
        return getTarget(getTargetId());
    }

    @Internal
    /** Cursors already have the target ID, so no need for reflection. */
    public TARGET getTarget(long targetId) {
        synchronized (this) {
            if (resolvedTargetId == targetId) {
                return target;
            }
        }

        ensureBoxes(null);
        // Do not synchronize while doing DB stuff
        TARGET targetNew = targetBox.get(targetId);

        setResolvedTarget(targetNew, targetId);
        return targetNew;
    }

    private void ensureBoxes(TARGET target) {
        // Only check the property set last
        if (targetBox == null) {
            Field boxStoreField = ReflectionCache.getInstance().getField(entity.getClass(), "__boxStore");
            try {
                boxStore = (BoxStore) boxStoreField.get(entity);
                if (boxStore == null) {
                    if (target != null) {
                        boxStoreField = ReflectionCache.getInstance().getField(target.getClass(), "__boxStore");
                        boxStore = (BoxStore) boxStoreField.get(target);
                    }
                    if (boxStore == null) {
                        throw new DbDetachedException("Cannot resolve relation for detached entities");
                    }
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
        return resolvedTargetId != getTargetId();
    }

    public boolean isResolvedAndNotNull() {
        return resolvedTargetId != 0 && resolvedTargetId != getTargetId();
    }

    public boolean isNull() {
        return getTargetId() == 0;
    }

    public void setTargetId(long targetId) {
        try {
            getTargetIdField().set(entity, targetId);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Could not update to-one ID in entity", e);
        }
    }

    void setAndUpdateTargetId(long targetId) {
        setTargetId(targetId);
        ensureBoxes(null);
        // TODO update on targetId in DB
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Sets the relation ID in the enclosed entity to the ID of the given target entity.
     * If the target entity was not put in the DB yet (its ID is 0), it will be put before to get its ID.
     */
    // TODO provide a overload with a ToMany parameter, which also gets updated
    public void setTarget(final TARGET target) {
        if (target != null) {
            ensureBoxes(target);
            long targetId = targetBox.getId(target);
            if (targetId == 0) {
                targetId = targetBox.put(target);
            }
            setTargetId(targetId);
            setResolvedTarget(target, targetId);
        } else {
            setTargetId(0);
            clearResolved();
        }
    }

    /**
     * Sets the relation ID in the enclosed entity to the ID of the given target entity and puts the enclosed entity.
     * If the target entity was not put in the DB yet (its ID is 0), it will be put before to get its ID.
     */
    // TODO provide a overload with a ToMany parameter, which also gets updated
    public void setAndPutTarget(final TARGET target) {
        ensureBoxes(target);
        if (target != null) {
            long targetId = targetBox.getId(target);
            if (targetId == 0) {
                setAndPutTargetAlways(target);
            } else {
                setTargetId(targetId);
                setResolvedTarget(target, targetId);
                entityBox.put(entity);
            }
        } else {
            setTargetId(0);
            clearResolved();
            entityBox.put(entity);
        }
    }

    /**
     * Sets the relation ID in the enclosed entity to the ID of the given target entity and puts both entities.
     */
    // TODO provide a overload with a ToMany parameter, which also gets updated
    public void setAndPutTargetAlways(final TARGET target) {
        ensureBoxes(target);
        if (target != null) {
            boxStore.runInTx(new Runnable() {
                @Override
                public void run() {
                    long targetKey = targetBox.put(target);
                    setResolvedTarget(target, targetKey);
                    entityBox.put(entity);
                }
            });
        } else {
            setTargetId(0);
            clearResolved();
            entityBox.put(entity);
        }
    }

    /** Both values should be set (and read) "atomically" using synchronized. */
    private synchronized void setResolvedTarget(TARGET target, long targetId) {
        resolvedTargetId = targetId;
        this.target = target;
    }

    /**
     * Clears the target.
     */
    private synchronized void clearResolved() {
        resolvedTargetId = 0;
        target = null;
    }

    // Future alternative: Implemented by generated ToOne sub classes
    public long getTargetId() {
        Field keyField = getTargetIdField();
        try {
            Long key = (Long) keyField.get(entity);
            return key != null ? key : 0;
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Could not access field " + keyField);
        }
    }

    private Field getTargetIdField() {
        if (targetIdField == null) {
            targetIdField = ReflectionCache.getInstance().getField(entity.getClass(), relationIdProperty.name);
        }
        return targetIdField;
    }
}
