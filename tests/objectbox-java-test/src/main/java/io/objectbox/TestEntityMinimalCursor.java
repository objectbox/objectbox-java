package io.objectbox;

import io.objectbox.annotation.apihint.Internal;
import io.objectbox.internal.CursorFactory;
import io.objectbox.internal.IdGetter;

public class TestEntityMinimalCursor extends Cursor<TestEntityMinimal> {
    private static final TestEntityMinimal_ PROPERTIES = new TestEntityMinimal_();

    @Internal
    static final class Factory implements CursorFactory<TestEntityMinimal> {
        public Cursor<TestEntityMinimal> createCursor(Transaction tx, long cursorHandle, BoxStore boxStoreForEntities) {
            return new TestEntityMinimalCursor(tx, cursorHandle, boxStoreForEntities);
        }
    }

    public TestEntityMinimalCursor(Transaction tx, long cursor, BoxStore boxStore) {
        super(tx, cursor, PROPERTIES, boxStore);
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
