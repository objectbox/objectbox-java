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

import android.content.Context;

import io.objectbox.BoxStore;

/**
 * A helper class to start the ObjectBox Admin web app used to browse and gain insights into the database.
 * <p>
 * Usage requires manually configuring some ObjectBox dependencies, see the
 * <a href="https://docs.objectbox.io/data-browser">documentation</a> for more details.
 *
 * <pre>
 * if (BuildConfig.DEBUG) {
 *     boolean started = new AndroidObjectBrowser(boxStore).start(this);
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
 *
 * @deprecated Use {@link Admin} instead.
 */
@Deprecated
public class AndroidObjectBrowser extends Admin {

    /**
     * Creates a helper to control Admin for the given {@code boxStore}.
     * <p>
     * See the {@link AndroidObjectBrowser class documentation} for details.
     *
     * @deprecated Use {@link Admin} instead.
     */
    @Deprecated
    public AndroidObjectBrowser(BoxStore boxStore) {
        super(boxStore);
    }

    /**
     * The id passed to {@link android.app.NotificationManager#notify} for the Admin notification.
     *
     * @deprecated Use {@link Admin} instead.
     */
    @Deprecated
    public int getNotificationId() {
        return super.getNotificationId();
    }

    /**
     * The id passed to {@link android.app.NotificationManager#notify} for the Admin notification.
     *
     * @deprecated Use {@link Admin} instead.
     */
    @Deprecated
    public void setNotificationId(int notificationId) {
        super.setNotificationId(notificationId);
    }

    /**
     * Starts the ObjectBox Admin HTTP server.
     * <p>
     * See the {@link AndroidObjectBrowser class documentation} for details.
     *
     * @deprecated Use {@link Admin} instead.
     */
    @Deprecated
    public boolean start(Context context) {
        return super.start(context);
    }
}
