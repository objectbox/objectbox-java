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

/**
 * Handles requesting Android runtime permissions required for Mesh Sync.
 *
 * Usage:
 * 1. Create an instance and keep it for the lifetime of the Activity.
 * 2. Call [requestIfMissing] with the Activity and a callback to run once permissions are handled.
 * 3. Forward [Activity.onRequestPermissionsResult] results to [onRequestPermissionsResult].
 *
 * If your app already [requests permissions](https://developer.android.com/training/permissions/requesting)
 * for other purposes, it might want to use [missingRuntimePermissions] or [runtimePermissions]
 * instead.
 */
class MeshSyncPermissions {

    private val pendingCallbacks = mutableListOf<() -> Unit>()

    /**
     * Requests any missing Mesh Sync runtime permissions, then invokes [callback].
     *
     * If all permissions are already granted, [callback] is invoked immediately.
     * If a permission request is already in flight, [callback] is queued and invoked
     * together with the other pending callbacks once the result arrives.
     */
    fun requestIfMissing(activity: Activity, callback: () -> Unit) {
        val missingPermissions = missingRuntimePermissions(activity)
        if (missingPermissions.isEmpty()) {
            callback()
            return
        }

        pendingCallbacks += callback
        if (pendingCallbacks.size > 1) return

        // Before M, missingRuntimePermissions above would return an empty list
        // and avoid calling this. But guard additionally for safety and to
        // avoid a Lint error.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            activity.requestPermissions(missingPermissions.toTypedArray(), PERMISSIONS_REQUEST_CODE)
        }
    }

    /**
     * Forward the result of [Activity.onRequestPermissionsResult] to this method.
     *
     * Returns `true` if the request code matches and callbacks were triggered.
     *
     * Note: this doesn't check if permissions were granted or if a rationale should be shown.
     */
    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ): Boolean {
        if (requestCode != PERMISSIONS_REQUEST_CODE) return false

        val callbacks = pendingCallbacks.toList()
        pendingCallbacks.clear()
        callbacks.forEach { it() }
        return true
    }

    /**
     * Returns the list of Mesh Sync runtime permissions that have not yet been granted.
     */
    fun missingRuntimePermissions(activity: Activity): List<String> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return emptyList() // No runtime permissions before API 23
        }

        return runtimePermissions()
            .filter { activity.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
    }

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

    companion object {
        /** Request code used when calling [Activity.requestPermissions]. */
        const val PERMISSIONS_REQUEST_CODE = 0x0B09
    }
}
