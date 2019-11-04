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

package io.objectbox.internal;

import java.io.Closeable;

import io.objectbox.InternalAccess;
import io.objectbox.Transaction;
import io.objectbox.annotation.apihint.Beta;

/** Not intended for normal use. */
@Beta
public class DebugCursor implements Closeable {

    private final Transaction tx;
    private final long handle;
    private boolean closed;

    static native long nativeCreate(long txHandle);

    static native void nativeDestroy(long handle);

    static native byte[] nativeGet(long handle, byte[] key);

    static native byte[] nativeSeekOrNext(long handle, byte[] key);

    public static DebugCursor create(Transaction tx) {
        long txHandle = InternalAccess.getHandle(tx);
        return new DebugCursor(tx, nativeCreate(txHandle));
    }

    public DebugCursor(Transaction tx, long handle) {
        this.tx = tx;
        this.handle = handle;
    }

    @Override
    public synchronized void close() {
        if (!closed) {
            // Closeable recommendation: mark as closed before any code that might throw.
            closed = true;
            // tx is null despite check in constructor in some tests (called by finalizer):
            // Null check avoids NPE in finalizer and seems to stabilize Android instrumentation perf tests.
            if (tx != null && !tx.getStore().isClosed()) {
                nativeDestroy(handle);
            }
        }
    }

    /**
     * Explicitly call {@link #close()} instead to avoid expensive finalization.
     */
    @SuppressWarnings("deprecation") // finalize()
    @Override
    protected void finalize() throws Throwable {
        if (!closed) {
            close();
            super.finalize();
        }
    }

    public byte[] get(byte[] key) {
        return nativeGet(handle, key);
    }

    public byte[] seekOrNext(byte[] key) {
        return nativeSeekOrNext(handle, key);
    }


}
