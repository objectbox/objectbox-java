/*
 * Copyright 2017-2024 ObjectBox Ltd.
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

/**
 * Exposes internal APIs to tests and code in other packages.
 */
@Internal
public class InternalAccess {

    @Internal
    public static Transaction getActiveTx(BoxStore boxStore) {
        Transaction tx = boxStore.activeTx.get();
        if (tx == null) {
            throw new IllegalStateException("No active transaction");
        }
        tx.checkOpen();
        return tx;
    }

    @Internal
    public static long getHandle(Transaction tx) {
        return tx.internalHandle();
    }

    @Internal
    public static void setSyncClient(BoxStore boxStore, @Nullable SyncClient syncClient) {
        boxStore.setSyncClient(syncClient);
    }

    @Internal
    public static <T> Cursor<T> getWriter(Box<T> box) {
        return box.getWriter();
    }

    @Internal
    public static <T> Cursor<T> getActiveTxCursor(Box<T> box) {
        return box.getActiveTxCursor();
    }

    @Internal
    public static <T> long getActiveTxCursorHandle(Box<T> box) {
        return box.getActiveTxCursor().internalHandle();
    }

    @Internal
    public static <T> void commitWriter(Box<T> box, Cursor<T> writer) {
        box.commitWriter(writer);
    }

    /**
     * Makes creation more expensive, but lets Finalizers show the creation stack for dangling resources.
     * <p>
     * Currently used by integration tests.
     */
    @SuppressWarnings("unused")
    @Internal
    public static void enableCreationStackTracking() {
        Transaction.TRACK_CREATION_STACK = true;
        Cursor.TRACK_CREATION_STACK = true;
    }

    @Internal
    public static BoxStoreBuilder clone(BoxStoreBuilder original, String namePostfix) {
        return original.createClone(namePostfix);
    }
}
