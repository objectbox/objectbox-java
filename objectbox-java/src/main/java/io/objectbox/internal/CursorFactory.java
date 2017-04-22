package io.objectbox.internal;

import javax.annotation.Nullable;

import io.objectbox.BoxStore;
import io.objectbox.Cursor;
import io.objectbox.Transaction;
import io.objectbox.annotation.apihint.Internal;

@Internal
public interface CursorFactory<T> {
    Cursor<T> createCursor(Transaction tx, long cursorHandle, @Nullable BoxStore boxStore);
}
