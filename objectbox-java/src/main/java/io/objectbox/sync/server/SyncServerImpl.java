/*
 * Copyright 2019-2024 ObjectBox Ltd.
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

package io.objectbox.sync.server;

import java.net.URI;
import java.net.URISyntaxException;

import javax.annotation.Nullable;

import io.objectbox.annotation.apihint.Internal;
import io.objectbox.sync.listener.SyncChangeListener;

/**
 * Internal sync server implementation. Use {@link SyncServer} to access functionality,
 * this class may change without notice.
 */
@Internal
public final class SyncServerImpl implements SyncServer {

    private final URI url;
    private volatile long handle;

    /**
     * Protects listener instance from garbage collection.
     */
    @SuppressWarnings("unused")
    @Nullable
    private volatile SyncChangeListener syncChangeListener;

    SyncServerImpl(SyncServerBuilder builder) {
        this.url = builder.url;

        long storeHandle = builder.boxStore.getNativeStore();
        long handle = nativeCreateFromFlatOptions(storeHandle, builder.buildSyncServerOptions());
        if (handle == 0) {
            throw new RuntimeException("Failed to create sync server: handle is zero.");
        }
        this.handle = handle;

        if (builder.changeListener != null) {
            setSyncChangeListener(builder.changeListener);
        }
    }

    private long getHandle() {
        long handle = this.handle;
        if (handle == 0) {
            throw new IllegalStateException("SyncServer already closed");
        }
        return handle;
    }

    @Override
    public String getUrl() {
        try {
            return new URI(url.getScheme(), null, url.getHost(), getPort(), null, null, null).toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException("Server URL can not be constructed", e);
        }
    }

    @Override
    public int getPort() {
        return nativeGetPort(getHandle());
    }

    @Override
    public boolean isRunning() {
        long handle = this.handle;  // Do not call getHandle() as it throws if handle is 0
        return handle != 0 && nativeIsRunning(handle);
    }

    @Override
    public String getStatsString() {
        return nativeGetStatsString(getHandle());
    }

    @Override
    public void setSyncChangeListener(@Nullable SyncChangeListener changesListener) {
        this.syncChangeListener = changesListener;
        nativeSetSyncChangesListener(getHandle(), changesListener);
    }

    @Override
    public void start() {
        nativeStart(getHandle());
    }

    @Override
    public void stop() {
        nativeStop(getHandle());
    }

    @Override
    public void close() {
        long handleToDelete = handle;
        handle = 0;
        if (handleToDelete != 0) {
            nativeDelete(handleToDelete);
        }
    }

    /**
     * Users of this class should explicitly call {@link #close()} instead to avoid expensive finalization.
     */
    @SuppressWarnings("deprecation") // finalize()
    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

    /**
     * Creates a native Sync server instance with FlatBuffer {@link SyncServerOptions} {@code flatOptionsByteArray}.
     *
     * @return The handle of the native server instance.
     */
    private static native long nativeCreateFromFlatOptions(long storeHandle, byte[] flatOptionsByteArray);

    private native void nativeDelete(long handle);

    private native void nativeStart(long handle);

    private native void nativeStop(long handle);

    private native boolean nativeIsRunning(long handle);

    private native int nativeGetPort(long handle);

    private native String nativeGetStatsString(long handle);

    private native void nativeSetSyncChangesListener(long handle, @Nullable SyncChangeListener changesListener);

}
