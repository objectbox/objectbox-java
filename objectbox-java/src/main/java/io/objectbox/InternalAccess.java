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

import io.objectbox.annotation.apihint.Internal;

@Internal
public class InternalAccess {
    public static <T> Cursor<T> getReader(Box<T> box) {
        return box.getReader();
    }

    public static long getHandle(Cursor reader) {
        return reader.internalHandle();
    }

    public static long getHandle(Transaction tx) {
        return tx.internalHandle();
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

    /** Makes creation more expensive, but lets Finalizers show the creation stack for dangling resources. */
    public static void enableCreationStackTracking() {
        Transaction.TRACK_CREATION_STACK = true;
        Cursor.TRACK_CREATION_STACK = true;
    }
}
