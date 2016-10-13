package io.objectbox.query;

import java.util.List;

import io.objectbox.Box;

/**
 * Created by Markus on 13.10.2016.
 */
public class Query<T> {
    private native static Object nativefindFirst(long handle, long cursorHandle);
    private native static Object nativefindUnique(long handle, long cursorHandle);
    private native static List nativeFindAll(long handle, long cursorHandle);

    private final Box<T> box;
    private final long handle;

    public Query(Box<T> box, long queryHandle) {
        this.box = box;
        handle = queryHandle;
    }

    public T findFirst() {
        long cursorHandle = box.internalReaderHandle();
        return (T) nativefindFirst(handle, cursorHandle);
    }

}
