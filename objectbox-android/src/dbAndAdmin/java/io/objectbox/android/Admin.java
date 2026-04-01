/*
 * Copyright 2022 ObjectBox Ltd. All rights reserved.
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

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Process;
import android.util.Log;

import io.objectbox.BoxStore;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

/**
 * A helper class to start the ObjectBox Admin web app used to browse and gain insights into the database.
 * <p>
 * Usage requires manually configuring some ObjectBox dependencies, see the
 * <a href="https://docs.objectbox.io/data-browser">documentation</a> for more details.
 *
 * <pre>
 * if (BuildConfig.DEBUG) {
 *     boolean started = new Admin(boxStore).start(this);
 *     Log.i("ObjectBoxAdmin", "Started: " + started);
 * }
 * </pre>
 * After {@link #start} is called a notification is displayed. Tap it to open this Admin URL on the device.
 * Alternatively, look for a logcat message from Admin to obtain the URL. Use {@code adb forward} to access
 * the URL on your development machine.
 * <p>
 * Tapping the notification starts a foreground service to keep this app running in the background.
 * Stop this keep-alive service from the notification.
 * <p>
 * See the web <a href="https://docs.objectbox.io/data-browser">documentation</a> for details.
 */
public class Admin {

    private static final String TAG = "ObjectBoxAdmin";
    private static final String NOTIFICATION_CHANNEL_ID = "objectbox-browser";

    private final BoxStore boxStore;
    private int notificationId;

    /**
     * Creates a helper to control Admin for the given {@code boxStore}.
     * <p>
     * See the {@link Admin class documentation} for details.
     */
    public Admin(BoxStore boxStore) {
        this.boxStore = boxStore;
    }

    /**
     * The id passed to {@link android.app.NotificationManager#notify} for the Admin notification.
     */
    public int getNotificationId() {
        return notificationId;
    }

    /**
     * The id passed to {@link android.app.NotificationManager#notify} for the Admin notification.
     */
    public void setNotificationId(int notificationId) {
        this.notificationId = notificationId;
    }

    /**
     * Starts the ObjectBox Admin HTTP server.
     * <p>
     * See the {@link Admin class documentation} for details.
     */
    public boolean start(Context context) {
        if (!BoxStore.isObjectBrowserAvailable()) {
            return false;
        }

        // compare with objectbox-android-objectbrowser/src/main/AndroidManifest.xml
        // SecurityException if no INTERNET permission
        context.enforcePermission(Manifest.permission.INTERNET, Process.myPid(), Process.myUid(), null);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                && context.getApplicationInfo().targetSdkVersion >= Build.VERSION_CODES.P) {
            // SecurityException if no FOREGROUND_SERVICE permission
            context.enforcePermission(Manifest.permission.FOREGROUND_SERVICE, Process.myPid(), Process.myUid(), null);
        }

        int alreadyRunningPort = boxStore.getObjectBrowserPort();
        if (alreadyRunningPort != 0) {
            Log.w(TAG, "ObjectBox Admin is already running at port " + alreadyRunningPort);
            return false;
        }
        String url = boxStore.startObjectBrowser();
        if (url == null) {
            return false;
        }
        Log.i(TAG, "ObjectBox Admin running at URL: " + url);
        int port = boxStore.getObjectBrowserPort();
        Log.i(TAG, "To access the ObjectBox Admin URL on your machine run: adb forward tcp:" + port + " tcp:" + port);


        if (notificationId == 0) {
            notificationId = 19770000 + port;
        }

        // build intent and show notification
        Intent intent = new Intent(context, AdminNotificationReceiver.class);
        intent.setAction(AdminNotificationReceiver.ACTION_KEEP_ALIVE);
        intent.putExtra(AdminKeepAliveService.EXTRA_KEY_URL, url);
        intent.putExtra(AdminKeepAliveService.EXTRA_KEY_PORT, port);
        intent.putExtra(AdminKeepAliveService.EXTRA_KEY_NOTIFICATION_ID, notificationId);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent,
                buildPendingIntentFlags(PendingIntent.FLAG_CANCEL_CURRENT));

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        // Note: may be null in case where notification service is not available on device (e.g. custom Android OS).
        if (manager != null) {
            // On Android 13 or newer notifications for apps are turned off by default,
            // developers need to turn on notifications for the app through system settings
            // or request the permission from the user.
            // https://developer.android.com/develop/ui/views/notifications/notification-permission
            // Already on Android 7 or newer, notifications can been turned off in system settings. This may happen
            // while developers are testing an app, so also warn in this case.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !manager.areNotificationsEnabled()) {
                Log.w(TAG, "To use the ObjectBox Admin keep-alive notification turn on notifications for this app");
            }
            Notification.Builder builder = buildBaseNotification(context, port, manager);
            builder.setContentIntent(pendingIntent);
            manager.notify(notificationId, builder.build());
        }

        return true;
    }

    static Notification.Builder buildBaseNotification(Context context, int port, NotificationManager manager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Note: IMPORTANCE_LOW so no sound is played to avoid distractions while testing.
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,
                    "ObjectBox Admin", NotificationManager.IMPORTANCE_LOW);
            // if channel already exists, create call will be ignored
            manager.createNotificationChannel(channel);
        }

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(context, NOTIFICATION_CHANNEL_ID);
        } else {
            builder = new Notification.Builder(context);
        }

        builder.setContentTitle(context.getString(R.string.objectbox_objectBrowserNotificationTitle))
                .setContentText(context.getString(R.string.objectbox_objectBrowserNotificationText, port))
                .setSmallIcon(R.drawable.objectbox_notification);

        return builder;
    }

    static Intent viewIntent(String url) {
        Intent viewIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        viewIntent.setFlags(FLAG_ACTIVITY_NEW_TASK);
        return viewIntent;
    }

    /**
     * Targeting Android 12 requires to mark PendingIntents explicitly as immutable or mutable.
     * https://developer.android.com/about/versions/12/behavior-changes-12#pending-intent-mutability
     */
    static int buildPendingIntentFlags(int flags) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return flags | PendingIntent.FLAG_IMMUTABLE;
        } else {
            return flags;
        }
    }

}
