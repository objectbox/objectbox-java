/*
 * Copyright 2017 ObjectBox Ltd. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.objectbox;

import java.io.Closeable;

import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
@SuppressWarnings("WeakerAccess,UnusedReturnValue, unused")
public class KeyValueCursor implements Closeable {
    private static final int PUT_FLAG_FIRST = 1;
    private static final int PUT_FLAG_COMPLETE = 1 << 1;
    private static final int PUT_FLAG_INSERT_NEW = 1 << 2;

    static native void nativePutLongKey(long cursor, long key, byte[] value);

    static native void nativeDestroy(long cursor);

    static native byte[] nativeGetLongKey(long cursor, long key);

    static native byte[] nativeGetNext(long cursor);

    static native byte[] nativeGetFirst(long cursor);

    static native byte[] nativeGetLast(long cursor);

    static native byte[] nativeGetPrev(long cursor);

    static native byte[] nativeGetCurrent(long cursor);

    static native byte[] nativeGetEqualOrGreater(long cursor, long key);

    static native boolean nativeRemoveAt(long cursor, long key);

    static native boolean nativeSeek(long cursor, long key);

    static native long nativeGetKey(long cursor);

    static native void nativeGetKey(long cursor, long key);

    final private long cursor;

    public KeyValueCursor(long cursor) {
        this.cursor = cursor;
    }

    public void put(long key, byte[] data) {
        nativePutLongKey(cursor, key, data);
    }

    public byte[] get(long key) {
        return nativeGetLongKey(cursor, key);
    }

    public byte[] getNext() {
        return nativeGetNext(cursor);
    }

    public byte[] getFirst() {
        return nativeGetFirst(cursor);
    }

    public byte[] getLast() {
        return nativeGetLast(cursor);
    }

    public byte[] getPrev() {
        return nativeGetPrev(cursor);
    }

    public byte[] getEqualOrGreater(long key) {
        return nativeGetEqualOrGreater(cursor, key);
    }

    public byte[] getCurrent() {
        return nativeGetCurrent(cursor);
    }

    public long getKey() {
        return nativeGetKey(cursor);
    }

    public boolean seek(long key) {
        return nativeSeek(cursor, key);
    }

    public boolean removeAt(long key) {
        return nativeRemoveAt(cursor, key);
    }

    @Override
    public void close() {
        nativeDestroy(cursor);
    }

}
