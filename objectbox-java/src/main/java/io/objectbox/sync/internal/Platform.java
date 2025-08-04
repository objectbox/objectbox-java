/*
 * Copyright 2020 ObjectBox Ltd.
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

package io.objectbox.sync.internal;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.annotation.Nullable;

import io.objectbox.BoxStore;
import io.objectbox.sync.ConnectivityMonitor;

/**
 * Provides access to platform-specific features.
 */
public class Platform {

    public static Platform findPlatform() {
        // Android
        Object contextInstance = BoxStore.getContext();
        if (contextInstance != null) {
            Throwable throwable = null;

            // Note: do not catch Exception as it will swallow exceptions useful for debugging.
            // Also can't catch ReflectiveOperationException, is K+ (19+) on Android.
            // noinspection TryWithIdenticalCatches Requires Android K+ (19+).
            try {
                Class<?> contextClass = Class.forName("android.content.Context");
                Class<?> platformClass = Class.forName("io.objectbox.android.internal.AndroidPlatform");
                Method create = platformClass.getMethod("create", contextClass);
                return (Platform) create.invoke(null, contextInstance);
            } catch (NoSuchMethodException e) {
                throwable = e;
            } catch (IllegalAccessException e) {
                throwable = e;
            } catch (InvocationTargetException e) {
                throwable = e;
            } catch (ClassNotFoundException ignored) {
                // Android API or library not in classpath.
            }

            if (throwable != null) {
                throw new RuntimeException("AndroidPlatform could not be created.", throwable);
            }
        }

        return new Platform();
    }

    @Nullable
    public ConnectivityMonitor getConnectivityMonitor() {
        return null;
    }
}
