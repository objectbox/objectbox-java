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

import android.app.Activity
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class MeshSyncPermissionsTest {

    private lateinit var activity: Activity
    private lateinit var meshSyncPermissions: MeshSyncPermissions

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(Activity::class.java).create().get()
        meshSyncPermissions = MeshSyncPermissions()
    }

    @Test
    fun requestIfMissing_allGranted_callbackInvokedImmediately() {
        shadowOf(RuntimeEnvironment.getApplication())
            .grantPermissions(*meshSyncPermissions.runtimePermissions().toTypedArray())

        var callbackInvoked = false
        meshSyncPermissions.requestIfMissing(activity) { callbackInvoked = true }

        assertTrue(
            "Callback should be invoked immediately when all permissions are granted",
            callbackInvoked
        )
        assertNull(
            "requestPermissions should not have been called",
            shadowOf(activity).lastRequestedPermission
        )
    }

    @Test
    fun requestIfMissing_missing_requestedAndCallbacksInvokedOnResult() {
        var callback1Invoked = false
        var callback2Invoked = false
        meshSyncPermissions.requestIfMissing(activity) { callback1Invoked = true }
        meshSyncPermissions.requestIfMissing(activity) { callback2Invoked = true }

        // Neither callback should fire until signaling permission result
        assertFalse(callback1Invoked)
        assertFalse(callback2Invoked)

        val lastRequest = shadowOf(activity).lastRequestedPermission
        assertNotNull("requestPermissions should have been called on the Activity", lastRequest)
        assertEquals(
            "Request code should match PERMISSIONS_REQUEST_CODE",
            MeshSyncPermissions.PERMISSIONS_REQUEST_CODE,
            lastRequest!!.requestCode
        )

        // Signal permission result
        val handled = meshSyncPermissions.onRequestPermissionsResult(
            MeshSyncPermissions.PERMISSIONS_REQUEST_CODE,
            emptyArray(),
            intArrayOf()
        )

        assertTrue(handled)
        assertTrue("First callback should be invoked", callback1Invoked)
        assertTrue("Second callback should be invoked", callback2Invoked)
    }

    @Test
    fun onRequestPermissionsResult_wrongRequestCode_callbacksNotInvoked() {
        var callbackInvoked = false
        meshSyncPermissions.requestIfMissing(activity) { callbackInvoked = true }

        val handled =
            meshSyncPermissions.onRequestPermissionsResult(0x1234, emptyArray(), intArrayOf())

        assertFalse(handled)
        assertFalse(callbackInvoked)
    }
}

