package io.objectbox;


import io.objectbox.BoxStore;
import io.objectbox.Cursor;
import io.objectbox.Properties;
import io.objectbox.Transaction;
import io.objectbox.annotation.apihint.Internal;
import io.objectbox.annotation.apihint.Temporary;
import io.objectbox.internal.CursorFactory;

// THIS CODE IS based on GENERATED code BY ObjectBox

/**
 * Cursor for DB entity "TestEntity".
 */
public final class TestEntityCursor extends Cursor<TestEntity> {
    @Internal
    static final class Factory implements CursorFactory<TestEntity> {
        public Cursor<TestEntity> createCursor(Transaction tx, long cursorHandle, BoxStore boxStoreForEntities) {
            return new TestEntityCursor(tx, cursorHandle, boxStoreForEntities);
        }
    }

    private static final TestEntity_ PROPERTIES = new TestEntity_();

    private static final TestEntity_.TestEntityIdGetter ID_GETTER = PROPERTIES.__ID_GETTER;


    // Property IDs get verified in Cursor base class
    private final static int __ID_simpleBoolean = TestEntity_.simpleBoolean.id;
    private final static int __ID_simpleByte = TestEntity_.simpleByte.id;
    private final static int __ID_simpleShort = TestEntity_.simpleShort.id;
    private final static int __ID_simpleInt = TestEntity_.simpleInt.id;
    private final static int __ID_simpleLong = TestEntity_.simpleLong.id;
    private final static int __ID_simpleFloat = TestEntity_.simpleFloat.id;
    private final static int __ID_simpleDouble = TestEntity_.simpleDouble.id;
    private final static int __ID_simpleString = TestEntity_.simpleString.id;
    private final static int __ID_simpleByteArray = TestEntity_.simpleByteArray.id;

    public TestEntityCursor(Transaction tx, long cursor, BoxStore boxStore) {
        super(tx, cursor, PROPERTIES, boxStore);
    }

    @Override
    public final long getId(TestEntity entity) {
        return ID_GETTER.getId(entity);
    }

    /**
     * Puts an object into its box.
     *
     * @return The ID of the object within its box.
     */
    @Override
    public final long put(TestEntity entity) {
        long __assignedId = collect313311(cursor, entity.getId(), PUT_FLAG_FIRST | PUT_FLAG_COMPLETE,
                9, entity.getSimpleString(), 0, null, 0, null,
                10, entity.getSimpleByteArray(),
                0, 0, 6, entity.getSimpleLong(), 5, entity.getSimpleInt(),
                4, entity.getSimpleShort(), 3, entity.getSimpleByte(),
                2, entity.getSimpleBoolean() ? 1 : 0,
                7, entity.getSimpleFloat(), 8, entity.getSimpleDouble()
        );
        entity.setId(__assignedId);
        return __assignedId;
    }

    // TODO do we need this? @Override
    protected final boolean isEntityUpdateable() {
        return true;
    }

}
