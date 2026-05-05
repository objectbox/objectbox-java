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

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import androidx.annotation.Nullable;
import io.objectbox.sync.ConnectivityMonitor;

/**
 * A {@link ConnectivityMonitor} that registers a {@link AndroidNetworkStateReceiver}
 * while it has an observer set.
 */
class AndroidConnectivityMonitor extends ConnectivityMonitor {

    private final Context context;

    @Nullable
    private AndroidNetworkStateReceiver receiver;

    AndroidConnectivityMonitor(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public void onObserverSet() {
        AndroidNetworkStateReceiver receiver = new AndroidNetworkStateReceiver();
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        context.registerReceiver(receiver, filter);
        this.receiver = receiver;
    }

    @Override
    public void onObserverRemoved() {
        AndroidNetworkStateReceiver receiverToRemove = this.receiver;
        this.receiver = null;
        if (receiverToRemove != null) {
            context.unregisterReceiver(receiverToRemove);
        }
    }

    /**
     * Notifies the given monitor on the main thread if a network connection is available
     * after a {@link ConnectivityManager#CONNECTIVITY_ACTION} was broadcast.
     */
    class AndroidNetworkStateReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (!ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
                return; // Ignore unexpected broadcasts.
            }

            ConnectivityManager cm =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

            @SuppressLint("MissingPermission") // Permission checked before registering.
                    NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            boolean isConnected = activeNetwork != null && activeNetwork.isConnected();

            if (isConnected) {
                notifyConnectionAvailable();
            }
        }

    }

}
