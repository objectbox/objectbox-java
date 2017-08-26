package io.objectbox;

import io.objectbox.annotation.apihint.Internal;

@Internal
public class InternalAccess {
    public static <T> Cursor<T> getReader(Box<T> box) {
        return box.getReader();
    }

    public static long getHandle(Cursor reader) {
        return reader.internalHandle();
    }

    public static <T> void releaseReader(Box<T> box, Cursor<T> reader) {
        box.releaseReader(reader);
    }

    public static <T> Cursor<T> getWriter(Box<T> box) {
        return box.getWriter();
    }

    public static <T> Cursor<T> getActiveTxCursor(Box<T> box) {
        return box.getActiveTxCursor();
    }

    public static <T> long getActiveTxCursorHandle(Box<T> box) {
        return box.getActiveTxCursor().internalHandle();
    }

    public static <T> void releaseWriter(Box<T> box, Cursor<T> writer) {
        box.releaseWriter(writer);
    }

    public static <T> void commitWriter(Box<T> box, Cursor<T> writer) {
        box.commitWriter(writer);
    }
}
