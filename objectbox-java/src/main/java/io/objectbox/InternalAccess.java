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

import javax.annotation.Nullable;

import io.objectbox.annotation.apihint.Internal;
import io.objectbox.sync.SyncClient;

@Internal
public class InternalAccess {

    public static Transaction getActiveTx(BoxStore boxStore) {
        Transaction tx = boxStore.activeTx.get();
        if (tx == null) {
            throw new IllegalStateException("No active transaction");
        }
        tx.checkOpen();
        return tx;
    }

    public static long getHandle(Transaction tx) {
        return tx.internalHandle();
    }

    public static void setSyncClient(BoxStore boxStore, @Nullable SyncClient syncClient) {
        boxStore.setSyncClient(syncClient);
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

    public static <T> void commitWriter(Box<T> box, Cursor<T> writer) {
        box.commitWriter(writer);
    }

    /** Makes creation more expensive, but lets Finalizers show the creation stack for dangling resources. */
    public static void enableCreationStackTracking() {
        Transaction.TRACK_CREATION_STACK = true;
        Cursor.TRACK_CREATION_STACK = true;
    }
}
