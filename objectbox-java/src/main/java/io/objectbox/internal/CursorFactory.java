package io.objectbox.internal;

import io.objectbox.Cursor;
import io.objectbox.Transaction;
import io.objectbox.annotation.apihint.Internal;

@Internal
// TODO use me
public interface CursorFactory<T> {
    Cursor<T> createCursor(Transaction tx, long cursorHandle);
}
