package io.objectbox;

public class TestEntityMinimalCursor extends Cursor<TestEntityMinimal> {

    public TestEntityMinimalCursor(Transaction tx, long cursor) {
        super(tx, cursor, new DummyProperties());
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

    private static class DummyProperties implements Properties {
        @Override
        public Property[] getAllProperties() {
            return new Property[0];
        }

        @Override
        public Property getIdProperty() {
            return null;
        }

        @Override
        public String getDbName() {
            return null;
        }
    }
}
