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
import io.objectbox.sync.MeshSync
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
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
        meshSyncPermissions = MeshSyncPermissions(activity)
    }

    private fun grantAllPermissions() {
        shadowOf(RuntimeEnvironment.getApplication())
            .grantPermissions(*MeshSyncPermissions.runtimePermissions().toTypedArray())
    }

    @Test
    fun requestIfMissing_allGranted_permissionsNotRequested() {
        grantAllPermissions()

        meshSyncPermissions.requestIfMissing()

        assertNull(
            "requestPermissions should not have been called when all permissions are granted",
            shadowOf(activity).lastRequestedPermission
        )
    }

    @Test
    fun requestIfMissing_missing_requestsPermissions() {
        meshSyncPermissions.requestIfMissing()

        val lastRequest = shadowOf(activity).lastRequestedPermission
        assertNotNull("requestPermissions should have been called on the Activity", lastRequest)
        assertEquals(
            "Request code should match PERMISSIONS_REQUEST_CODE",
            MeshSyncPermissions.PERMISSIONS_REQUEST_CODE,
            lastRequest!!.requestCode
        )
    }

    private fun assertRetryNetworksIfPermissionsGranted(
        requestCode: Int = MeshSyncPermissions.PERMISSIONS_REQUEST_CODE,
        expectedHandled: Boolean,
        expectRetryNetworksCalled: Boolean
    ) {
        val meshSync = mock(MeshSync::class.java)
        val handled = meshSyncPermissions.retryNetworksIfPermissionsGranted(requestCode, meshSync)
        assertEquals(expectedHandled, handled)
        if (expectRetryNetworksCalled) verify(meshSync).retryNetworks()
        else verify(meshSync, never()).retryNetworks()
    }

    @Test
    fun retryNetworksIfPermissionsGranted_allGranted_retryNetworksCalled() {
        grantAllPermissions()

        assertRetryNetworksIfPermissionsGranted(
            expectedHandled = true,
            expectRetryNetworksCalled = true
        )
    }

    @Test
    fun retryNetworksIfPermissionsGranted_oneGranted_retryNetworksCalled() {
        shadowOf(RuntimeEnvironment.getApplication())
            .grantPermissions(Manifest.permission.NEARBY_WIFI_DEVICES)

        assertRetryNetworksIfPermissionsGranted(
            expectedHandled = true,
            expectRetryNetworksCalled = true
        )
    }

    @Test
    fun retryNetworksIfPermissionsGranted_noneGranted_returnsFalse() {
        assertRetryNetworksIfPermissionsGranted(
            expectedHandled = false,
            expectRetryNetworksCalled = false
        )
    }

    @Test
    fun retryNetworksIfPermissionsGranted_wrongCode_returnsFalse() {
        // Not required with the current implementation, but things might change
        grantAllPermissions()

        assertRetryNetworksIfPermissionsGranted(
            requestCode = 0x1234,
            expectedHandled = false,
            expectRetryNetworksCalled = false
        )
    }

}
