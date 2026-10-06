/*
 * Copyright 2026 ObjectBox Ltd. <https://objectbox.io>
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

import android.app.Notification;
import android.app.Service;
import android.content.pm.ServiceInfo;
import android.os.Build;

/**
 * Provides compatibility for foreground service APIs introduced after Android 5.0.
 */
public final class ServiceCompat {

    private ServiceCompat() {
    }

    /**
     * Starts the service using the type defined in the manifest.
     */
    public static void startForeground(Service service, int notificationId, Notification notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            service.startForeground(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST);
        } else {
            service.startForeground(notificationId, notification);
        }
    }

    /**
     * Stops the service and removes the notification.
     */
    public static void stopForeground(Service service) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            service.stopForeground(Service.STOP_FOREGROUND_REMOVE);
        } else {
            stopForegroundLegacy(service);
        }
    }

    @SuppressWarnings("deprecation")
    private static void stopForegroundLegacy(Service service) {
        service.stopForeground(true);
    }

}
