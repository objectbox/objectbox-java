/*
 * Copyright 2020 ObjectBox Ltd. All rights reserved.
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

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.GrantPermissionRule;
import io.objectbox.android.internal.AndroidPlatform;

import static org.junit.Assert.assertNotNull;

@RunWith(AndroidJUnit4.class)
public class AndroidPlatformTest {

    @Rule
    public GrantPermissionRule mRuntimePermissionRule = GrantPermissionRule
            .grant(Manifest.permission.ACCESS_NETWORK_STATE);

    @Test
    public void builds() {
        AndroidPlatform platform = AndroidPlatform.create(ApplicationProvider.getApplicationContext());
        assertNotNull(platform);
        assertNotNull(platform.getConnectivityMonitor());
    }
}
