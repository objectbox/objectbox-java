package io.objectbox;

public class TestEntityMinimalCursor extends Cursor<TestEntityMinimal> {

    public TestEntityMinimalCursor(Transaction tx, long cursor) {
        super(tx, cursor, null);
    }

    @Override
    protected long getId(TestEntityMinimal entity) {
        return entity.getId();
    }

    public long put(TestEntityMinimal entity) {
        long key = entity.getId();
        key = collect313311(cursor, key, PUT_FLAG_FIRST | PUT_FLAG_COMPLETE,
                2, entity.getText(), 0, null, 0, null,
                0, null, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        );
        entity.setId(key);
        return key;
    }
}
