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

package io.objectbox.meshsync.android;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import io.objectbox.exception.FeatureNotAvailableException;


import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

/**
 * Tests run on the JVM using Robolectric against the JVM native library (which does not include
 * Sync).
 * <p>
 * Tests requiring the Sync feature and the Nearby Connections API run as instrumented tests against
 * the Android native library (currently in the internal repository).
 */
@RunWith(RobolectricTestRunner.class)
public class AndroidMeshSyncTest {

    @Test
    public void createConfig_withoutSyncLibrary_throws() {
        // The JVM test library does not include Sync
        assumeFalse(io.objectbox.BoxStore.isSyncAvailable());

        Context context = ApplicationProvider.getApplicationContext();
        FeatureNotAvailableException exception = assertThrows(
                FeatureNotAvailableException.class,
                () -> AndroidMeshSync.createConfig(context, "test-mesh")
        );
        String message = exception.getMessage();
        assertTrue(message, message.contains("objectbox-sync-android")
                && message.contains("https://objectbox.io/sync"));
    }

    @Test
    public void createConfig_nullContext_throws() {
        assertThrows(IllegalArgumentException.class, () -> AndroidMeshSync.createConfig(null, "test-mesh"));
    }

}
