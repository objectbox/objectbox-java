package io.objectbox;

public class TestEntityCursor extends Cursor<TestEntity> {

    public TestEntityCursor(Transaction tx, long cursor) {
        super(tx, cursor, new TestEntity_());
    }

    @Override
    protected long getId(TestEntity entity) {
        return entity.getId();
    }

    public long put(TestEntity entity) {
        long key = entity.getId();
        key = collect313311(cursor, key, PUT_FLAG_FIRST | PUT_FLAG_COMPLETE,
                9, entity.getSimpleString(), 0, null, 0, null,
                10, entity.getSimpleByteArray(),
                0, 0, 6, entity.getSimpleLong(), 5, entity.getSimpleInt(),
                4, entity.getSimpleShort(), 3, entity.getSimpleByte(),
                2, entity.getSimpleBoolean() ? 1 : 0,
                7, entity.getSimpleFloat(), 8, entity.getSimpleDouble()
        );
        entity.setId(key);
        return key;
    }
}
