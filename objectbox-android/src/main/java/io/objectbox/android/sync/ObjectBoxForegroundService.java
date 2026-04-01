/*
 * Copyright 2021 ObjectBox Ltd. All rights reserved.
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

package io.objectbox.android.sync;

import android.Manifest;
import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.Process;
import android.util.Log;

import androidx.annotation.Nullable;

/**
 * A no-op foreground {@link Service} to make it less likely an app is killed by the system.
 * Use {@link #start} and {@link #stop} to control the service.
 */
public class ObjectBoxForegroundService extends Service {

    public static final String ACTION_START = "obx_foreground_start";
    public static final String ACTION_STOP = "obx_foreground_stop";
    private static final String TAG = "OBX_FOREGROUND";

    private static int notificationId;
    @Nullable
    private static Notification notification;

    public static void start(Context context, int notificationId, Notification notification) {
        // Require FOREGROUND_SERVICE permission on Android 9 (API level 28)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                && context.getApplicationInfo().targetSdkVersion >= Build.VERSION_CODES.P) {
            // SecurityException if no FOREGROUND_SERVICE permission
            context.enforcePermission(Manifest.permission.FOREGROUND_SERVICE, Process.myPid(), Process.myUid(), null);
        }

        ObjectBoxForegroundService.notificationId = notificationId;
        ObjectBoxForegroundService.notification = notification;
        Intent intent = new Intent(context, ObjectBoxForegroundService.class);
        intent.setAction(ACTION_START);
        startServiceIntent(context, intent);
    }

    public static void stop(Context context) {
        Intent intent = new Intent(context, ObjectBoxForegroundService.class);
        intent.setAction(ACTION_STOP);
        startServiceIntent(context, intent);
    }

    private static void startServiceIntent(Context context, Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (ACTION_STOP.equals(intent.getAction())) {
            Log.d(TAG, "Stopping...");
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        } else if (ACTION_START.equals(intent.getAction())) {
            Log.d(TAG, "Starting...");
            int notificationId = ObjectBoxForegroundService.notificationId;
            Notification notification = ObjectBoxForegroundService.notification;
            if (notificationId == 0 || notification == null) {
                throw new IllegalArgumentException("No arguments given: notificationId or notification not set.");
            }
            startForeground(notificationId, notification);
            // Note: with START_STICKY would not get intent on restart.
            return START_REDELIVER_INTENT;
        } else {
            Log.w(TAG, "Ignoring start command: no action specified.");
            return START_NOT_STICKY;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
