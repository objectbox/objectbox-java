package io.objectbox.query;

import java.util.List;

import io.objectbox.Box;
import io.objectbox.annotation.apihint.Beta;

/**
 * Created by Markus on 13.10.2016.
 */
@Beta
public class Query<T> {
    private native static Object nativeFindFirst(long handle, long cursorHandle);
    private native static Object nativeFindUnique(long handle, long cursorHandle);
    private native static List nativeFind(long handle, long cursorHandle, long offset, long limit);
    private native static long nativeCount(long handle, long cursorHandle);

    private final Box<T> box;
    private final long handle;

    public Query(Box<T> box, long queryHandle) {
        this.box = box;
        handle = queryHandle;
    }

    public T findFirst() {
        long cursorHandle = box.internalReaderHandle();
        return (T) nativeFindFirst(handle, cursorHandle);
    }

    public T findUnique() {
        long cursorHandle = box.internalReaderHandle();
        return (T) nativeFindUnique(handle, cursorHandle);
    }

    public List<T> find() {
        long cursorHandle = box.internalReaderHandle();
        return nativeFind(handle, cursorHandle, 0, 0);
    }

    public List<T> find(long offset, long limit) {
        long cursorHandle = box.internalReaderHandle();
        return nativeFind(handle, cursorHandle, offset, limit);
    }

    public long count() {
        long cursorHandle = box.internalReaderHandle();
        return nativeCount(handle, cursorHandle);
    }

}
