/*
 * Copyright 2017 ObjectBox Ltd. All rights reserved.
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

import android.app.Notification;
import android.app.Notification.Action.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.IBinder;
import android.util.Log;

import javax.annotation.Nullable;

/**
 * Foreground service to keep app alive which displays a notification to view {@link Admin} URL or stop this service.
 */
public class AdminKeepAliveService extends Service {

    private static final String ACTION_STOP = "objectBox_keepAliveStop";

    static final String EXTRA_KEY_PORT = "port";
    static final String EXTRA_KEY_URL = "url";
    static final String EXTRA_KEY_NOTIFICATION_ID = "notificationId";

    private static final String TAG = AdminKeepAliveService.class.getSimpleName();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (ACTION_STOP.equals(intent.getAction())) {
            Log.d(TAG, "Stopping");
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }
        String url = intent.getStringExtra(EXTRA_KEY_URL);
        int port = intent.getIntExtra(EXTRA_KEY_PORT, 0);
        int notificationId = intent.getIntExtra(EXTRA_KEY_NOTIFICATION_ID, 0);

        if (url != null && url.startsWith("http") && port > 0 && notificationId > 0) {
            Intent stopIntent = new Intent(this, getClass());
            stopIntent.setAction(ACTION_STOP);
            PendingIntent stopPendingIntent = PendingIntent.getService(this, 0, stopIntent,
                    Admin.buildPendingIntentFlags(PendingIntent.FLAG_CANCEL_CURRENT));
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                    Admin.viewIntent(url), Admin.buildPendingIntentFlags(0));

            NotificationManager manager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            Notification.Builder builder = Admin.buildBaseNotification(this, port, manager);
            builder.setContentIntent(pendingIntent);
            // Actually useless because Foreground notifications cannot be deleted
            builder.setDeleteIntent(stopPendingIntent);
            builder.addAction(new Builder(R.drawable.objectbox_stop, "Stop", stopPendingIntent).build());

            startForeground(notificationId, builder.getNotification());
            Log.d(TAG, "Started");
            return START_REDELIVER_INTENT; // with START_STICKY would not get intent on restart
        } else {
            Log.w(TAG, "Ignoring start command due to incomplete data");
            return START_NOT_STICKY;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
