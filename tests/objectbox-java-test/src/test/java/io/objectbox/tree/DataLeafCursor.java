package io.objectbox.tree;

import io.objectbox.BoxStore;
import io.objectbox.Cursor;
import io.objectbox.annotation.apihint.Internal;
import io.objectbox.internal.CursorFactory;
import io.objectbox.relation.ToOne;

// THIS CODE IS GENERATED BY ObjectBox, DO NOT EDIT.

/**
 * ObjectBox generated Cursor implementation for "DataLeaf".
 * Note that this is a low-level class: usually you should stick to the Box class.
 */
public final class DataLeafCursor extends Cursor<DataLeaf> {
    @Internal
    static final class Factory implements CursorFactory<DataLeaf> {
        @Override
        public Cursor<DataLeaf> createCursor(io.objectbox.Transaction tx, long cursorHandle, BoxStore boxStoreForEntities) {
            return new DataLeafCursor(tx, cursorHandle, boxStoreForEntities);
        }
    }

    private static final DataLeaf_.DataLeafIdGetter ID_GETTER = DataLeaf_.__ID_GETTER;


    private final static int __ID_valueInt = DataLeaf_.valueInt.id;
    private final static int __ID_valueDouble = DataLeaf_.valueDouble.id;
    private final static int __ID_valueString = DataLeaf_.valueString.id;
    private final static int __ID_valueStrings = DataLeaf_.valueStrings.id;
    private final static int __ID_dataBranchId = DataLeaf_.dataBranchId.id;
    private final static int __ID_metaLeafId = DataLeaf_.metaLeafId.id;

    public DataLeafCursor(io.objectbox.Transaction tx, long cursor, BoxStore boxStore) {
        super(tx, cursor, DataLeaf_.__INSTANCE, boxStore);
    }

    @Override
    public long getId(DataLeaf entity) {
        return ID_GETTER.getId(entity);
    }

    /**
     * Puts an object into its box.
     *
     * @return The ID of the object within its box.
     */
    @Override
    public long put(DataLeaf entity) {
        ToOne<DataBranch> dataBranch = entity.dataBranch;
        if(dataBranch != null && dataBranch.internalRequiresPutTarget()) {
            Cursor<DataBranch> targetCursor = getRelationTargetCursor(DataBranch.class);
            try {
                dataBranch.internalPutTarget(targetCursor);
            } finally {
                targetCursor.close();
            }
        }
        ToOne<MetaLeaf> metaLeaf = entity.metaLeaf;
        if(metaLeaf != null && metaLeaf.internalRequiresPutTarget()) {
            Cursor<MetaLeaf> targetCursor = getRelationTargetCursor(MetaLeaf.class);
            try {
                metaLeaf.internalPutTarget(targetCursor);
            } finally {
                targetCursor.close();
            }
        }
        String[] valueStrings = entity.valueStrings;
        int __id4 = valueStrings != null ? __ID_valueStrings : 0;

        collectStringArray(cursor, 0, PUT_FLAG_FIRST,
                __id4, valueStrings);

        String valueString = entity.valueString;
        int __id3 = valueString != null ? __ID_valueString : 0;

        long __assignedId = collect313311(cursor, entity.id, PUT_FLAG_COMPLETE,
                __id3, valueString, 0, null,
                0, null, 0, null,
                __ID_valueInt, entity.valueInt, __ID_dataBranchId, entity.dataBranch.getTargetId(),
                __ID_metaLeafId, entity.metaLeaf.getTargetId(), 0, 0,
                0, 0, 0, 0,
                0, 0, __ID_valueDouble, entity.valueDouble);

        entity.id = __assignedId;

        attachEntity(entity);
        return __assignedId;
    }

    private void attachEntity(DataLeaf entity) {
        // Transformer will create __boxStore field in entity and init it here:
        // entity.__boxStore = boxStoreForEntities;
    }

}
