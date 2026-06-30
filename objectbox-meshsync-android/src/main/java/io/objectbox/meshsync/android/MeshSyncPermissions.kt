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

package io.objectbox.meshsync.android

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import io.objectbox.sync.MeshSync
import io.objectbox.sync.SyncClient

/**
 * Helps to request Android runtime permissions required for Mesh Sync.
 *
 * Usage:
 *
 * 1. Create an instance in the [Activity] that should be used to request permissions.
 * 2. Override [Activity.onRequestPermissionsResult] and call [retryNetworksIfPermissionsGranted].
 * 3. Call [requestIfMissing] to show permissions requests to the user.
 *
 * If your app already [requests permissions](https://developer.android.com/training/permissions/requesting)
 * for other purposes, it might want to use [missingRuntimePermissions] or [runtimePermissions]
 * instead.
 */
class MeshSyncPermissions(
    private val activity: Activity
) {

    /**
     * Requests any missing runtime permissions for Mesh Sync using the request code
     * [PERMISSIONS_REQUEST_CODE].
     *
     * Note: before calling this, your code might want to check [Activity.shouldShowRequestPermissionRationale]
     * if a rationale UI should be shown first. See how to
     * [request permissions](https://developer.android.com/training/permissions/requesting).
     */
    fun requestIfMissing() {
        val missingPermissions = missingRuntimePermissions()
        if (missingPermissions.isEmpty()) {
            return
        }

        // Before M, missingRuntimePermissions above would return an empty list
        // and avoid calling this. But guard additionally for safety and to
        // avoid a Lint error.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            activity.requestPermissions(missingPermissions.toTypedArray(), PERMISSIONS_REQUEST_CODE)
        }
    }

    /**
     * Returns `true` if the request code matches, all required permissions are granted and
     * [MeshSync.retryNetworks] was called.
     *
     * Call this from [Activity.onRequestPermissionsResult] and pass the received [requestCode].
     *
     * Note: your code can also check itself if all [missingRuntimePermissions] are granted and then
     * call [MeshSync.retryNetworks] (or create a [SyncClient]) itself.
     */
    fun retryNetworksIfPermissionsGranted(
        requestCode: Int,
        meshSync: MeshSync?
    ): Boolean {
        if (requestCode != PERMISSIONS_REQUEST_CODE) {
            return false // Don't handle other requests
        }
        if (missingRuntimePermissions().isNotEmpty()) {
            return false // Required permissions not granted
        }

        // Immediately retry
        meshSync?.retryNetworks()
        return true
    }

    /**
     * Returns the list of Mesh Sync runtime permissions that have not yet been granted.
     */
    fun missingRuntimePermissions(): List<String> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return emptyList() // No runtime permissions before API 23
        }

        return runtimePermissions()
            .filter { activity.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
    }

    companion object {
        /** Request code used when calling [Activity.requestPermissions]. */
        const val PERMISSIONS_REQUEST_CODE = 0x0B09

        /**
         * Returns the full list of runtime permissions required for Mesh Sync on the current device.
         */
        fun runtimePermissions(): List<String> {
            val permissions = mutableListOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                permissions += Manifest.permission.BLUETOOTH_ADVERTISE
                permissions += Manifest.permission.BLUETOOTH_CONNECT
                permissions += Manifest.permission.BLUETOOTH_SCAN
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissions += Manifest.permission.NEARBY_WIFI_DEVICES
            }
            return permissions
        }
    }
}
