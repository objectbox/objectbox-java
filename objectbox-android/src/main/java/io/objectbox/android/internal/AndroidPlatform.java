/*
 * Copyright 2020 ObjectBox Ltd. <https://objectbox.io>
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

package io.objectbox.android.internal;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Process;

import androidx.annotation.Nullable;
import io.objectbox.sync.ConnectivityMonitor;
import io.objectbox.sync.internal.Platform;

/**
 * Provides Android-specific features.
 * <p>
 * Requires the {@link Manifest.permission#ACCESS_NETWORK_STATE ACCESS_NETWORK_STATE} permission.
 */
public class AndroidPlatform extends Platform {

    public static AndroidPlatform create(Context context) {
        return new AndroidPlatform(context.getApplicationContext());
    }

    private final ConnectivityMonitor connectivityMonitor;

    private AndroidPlatform(Context context) {
        // AndroidConnectivityMonitor requires the ACCESS_NETWORK_STATE permission.
        // Most apps have it, so just do not provide a connectivity monitor if not.
        if (context.checkPermission(Manifest.permission.ACCESS_NETWORK_STATE,
                Process.myPid(), Process.myUid()) == PackageManager.PERMISSION_GRANTED) {
            connectivityMonitor = new AndroidConnectivityMonitor(context);
        } else {
            connectivityMonitor = null;
        }
    }

    @Override
    @Nullable
    public ConnectivityMonitor getConnectivityMonitor() {
        return connectivityMonitor;
    }

}
