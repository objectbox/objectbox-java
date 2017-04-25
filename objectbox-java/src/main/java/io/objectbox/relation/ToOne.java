package io.objectbox.relation;

import java.lang.reflect.Field;

import javax.annotation.Nullable;

import io.objectbox.Box;
import io.objectbox.BoxStore;
import io.objectbox.Cursor;
import io.objectbox.annotation.apihint.Internal;
import io.objectbox.exception.DbDetachedException;
import io.objectbox.internal.ReflectionCache;

/**
 * Manages a to-one relation: resolves the target object, keeps the target Id in sync, etc.
 */
// TODO add more tests
// TODO not exactly thread safe
public class ToOne<TARGET> {
    private final Object entity;
    private final RelationInfo relationInfo;

    private BoxStore boxStore;
    private Box entityBox;
    private volatile Box<TARGET> targetBox;
    private Field targetIdField;

    /**
     * Resolved target entity is cached
     */
    protected TARGET target;

    protected long targetId;

    private volatile long resolvedTargetId;

    /** To avoid calls to {@link #getTargetId()}, which may involve expensive reflection. */
    private boolean checkIdOfTargetForPut;

    public ToOne(Object entity, RelationInfo relationInfo) {
        this.entity = entity;
        this.relationInfo = relationInfo;
    }

    public TARGET getTarget() {
        return getTarget(getTargetId());
    }

    /** If property backed, entities can pass the target ID to avoid reflection. */
    @Internal
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
            entityBox = boxStore.boxFor(relationInfo.sourceInfo.getEntityClass());
            targetBox = boxStore.boxFor(relationInfo.targetInfo.getEntityClass());
        }
    }

    public TARGET getCachedTarget() {
        return target;
    }

    public boolean isResolved() {
        return resolvedTargetId == getTargetId();
    }

    public boolean isResolvedAndNotNull() {
        return resolvedTargetId != 0 && resolvedTargetId == getTargetId();
    }

    public boolean isNull() {
        return getTargetId() == 0 && target == null;
    }

    public void setTargetId(long targetId) {
        if (relationInfo.targetIdProperty != null) {
            try {
                getTargetIdField().set(entity, targetId);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Could not update to-one ID in entity", e);
            }
        } else {
            this.targetId = targetId;
        }
        if (targetId != 0) {
            checkIdOfTargetForPut = false;
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
    public void setTarget(@Nullable final TARGET target) {
        if (target != null) {
            long targetId = relationInfo.targetInfo.getIdGetter().getId(target);
            checkIdOfTargetForPut = true;
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
    public void setAndPutTarget(@Nullable final TARGET target) {
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
    public void setAndPutTargetAlways(@Nullable final TARGET target) {
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
    private synchronized void setResolvedTarget(@Nullable TARGET target, long targetId) {
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

    public long getTargetId() {
        if (relationInfo.targetIdProperty != null) {
            // Future alternative: Implemented by generated ToOne sub classes to avoid reflection
            Field keyField = getTargetIdField();
            try {
                Long key = (Long) keyField.get(entity);
                return key != null ? key : 0;
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Could not access field " + keyField);
            }
        } else {
            return targetId;
        }
    }

    private Field getTargetIdField() {
        if (targetIdField == null) {
            targetIdField = ReflectionCache.getInstance().getField(entity.getClass(), relationInfo.targetIdProperty.name);
        }
        return targetIdField;
    }


    @Internal
    public boolean internalRequiresPutTarget() {
        return checkIdOfTargetForPut && target != null && getTargetId() == 0;
    }

    @Internal
    public void internalPutTarget(Cursor<TARGET> targetCursor) {
        checkIdOfTargetForPut = false;
        long id = targetCursor.put(target);
        setTargetId(id);
        setResolvedTarget(target, id);
    }
}
