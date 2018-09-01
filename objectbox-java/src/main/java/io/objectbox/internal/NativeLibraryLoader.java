/*
 * Copyright 2017 ObjectBox Ltd. All rights reserved.
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

package io.objectbox.internal;

import org.greenrobot.essentials.io.IoUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * Separate class, so we can mock BoxStore.
 */
public class NativeLibraryLoader {
    static {
        String libname = "objectbox-jni";
        String filename = libname + ".so";
        boolean isLinux = false;
        // For Android, os.name is also "Linux", so we need an extra check
        // Is not completely reliable (e.g. Vivo devices), see workaround on load failure
        // Note: can not use check for Android classes as testing frameworks (Robolectric)
        // may provide them on non-Android devices
        boolean android = System.getProperty("java.vendor").contains("Android");
        if (!android) {
            String osName = System.getProperty("os.name").toLowerCase();
            String sunArch = System.getProperty("sun.arch.data.model");
            String cpuArchPostfix = "32".equals(sunArch) ? "-x86" : "-x64";
            if (osName.contains("windows")) {
                libname += "-windows" + cpuArchPostfix;
                filename = libname + ".dll";
                checkUnpackLib(filename);
            } else if (osName.contains("linux")) {
                isLinux = true;
                libname += "-linux" + cpuArchPostfix;
                filename = "lib" + libname + ".so";
                checkUnpackLib(filename);
            } else if (osName.contains("mac")) {
                libname += "-macos" + cpuArchPostfix;
                filename = "lib" + libname + ".dylib";
                checkUnpackLib(filename);
            }
        }
        File file = new File(filename);
        if (file.exists()) {
            System.load(file.getAbsolutePath());
        } else {
            if (!android) {
                System.err.println("File not available: " + file.getAbsolutePath());
            }
            try {
                System.loadLibrary(libname);
            } catch (UnsatisfiedLinkError e) {
                if (!android && isLinux) {
                    // maybe is Android, but check failed: try loading Android lib
                    System.loadLibrary("objectbox-jni");
                } else {
                    throw e;
                }
            }
        }
    }

    private static void checkUnpackLib(String filename) {
        String path = "/native/" + filename;
        URL resource = NativeLibraryLoader.class.getResource(path);
        if (resource == null) {
            System.err.println("Not available in classpath: " + path);
        } else {
            File file = new File(filename);
            try {
                URLConnection urlConnection = resource.openConnection();
                int length = urlConnection.getContentLength();
                long lastModified = urlConnection.getLastModified();
                if (!file.exists() || file.length() != length || file.lastModified() != lastModified) {
                    InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                    try {
                        OutputStream out = new BufferedOutputStream(new FileOutputStream(file));
                        try {
                            IoUtils.copyAllBytes(in, out);
                        } finally {
                            IoUtils.safeClose(out);
                        }
                    } finally {
                        IoUtils.safeClose(in);
                    }
                    if (lastModified > 0) {
                        file.setLastModified(lastModified);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void ensureLoaded() {
    }
}
