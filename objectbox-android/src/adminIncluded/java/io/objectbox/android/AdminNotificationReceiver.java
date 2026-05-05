/*
 * Copyright 2017 ObjectBox Ltd. <https://objectbox.io>
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

package io.objectbox.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.util.Log;

/**
 * Handles actions of the {@link Admin} notification.
 */
public class AdminNotificationReceiver extends BroadcastReceiver {

    private static final String TAG = AdminNotificationReceiver.class.getSimpleName();
    static final String ACTION_KEEP_ALIVE = "io.objectbox.action.KEEP_ALIVE";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!ACTION_KEEP_ALIVE.equals(intent.getAction())) {
            return;
        }
        if (!intent.hasExtra(AdminKeepAliveService.EXTRA_KEY_URL)) {
            Log.w(TAG, "Ignoring keep alive intent due to incomplete data");
            return;
        }

        // start foreground service to keep app process alive
        Intent serviceIntent = new Intent(context, AdminKeepAliveService.class);
        serviceIntent.putExtras(intent);
        if (VERSION.SDK_INT >= VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }

        // launch browser
        String url = intent.getStringExtra(AdminKeepAliveService.EXTRA_KEY_URL);
        context.startActivity(Admin.viewIntent(url));
    }

}
