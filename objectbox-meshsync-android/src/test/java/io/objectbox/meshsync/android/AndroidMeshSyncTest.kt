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

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.objectbox.exception.FeatureNotAvailableException
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests run on the JVM using Robolectric against a JVM database library which does not include
 * Sync.
 *
 * The internal objectbox repository has instrumented tests requiring the Sync feature and the
 * Nearby Connections API run against the Android database library.
 */
@RunWith(RobolectricTestRunner::class)
class AndroidMeshSyncTest {

    @Test
    fun createConfig_withoutSyncLibrary_throws() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val exception = assertThrows(FeatureNotAvailableException::class.java) {
            AndroidMeshSync.createConfig(context, "test-mesh")
        }
        val message = exception.message!!
        assertTrue(message, message.contains("does not include the Sync feature"))
    }

}
