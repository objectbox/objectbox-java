/*
 * Copyright 2017 ObjectBox Ltd.
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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.Arrays;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import io.objectbox.BoxStore;
import org.greenrobot.essentials.io.IoUtils;

/**
 * Separate class, so we can mock BoxStore.
 */
public class NativeLibraryLoader {

    private static final String OBJECTBOX_JNI = "objectbox-jni";

    static {
        String libname = OBJECTBOX_JNI;
        String filename = libname + ".so";

        final String vendor = System.getProperty("java.vendor");
        final String osName = System.getProperty("os.name").toLowerCase();

        // Some Android devices are detected as neither Android or Linux below,
        // so assume Linux by default to always fallback to Android
        boolean isLinux = true;
        // For Android, os.name is also "Linux", so we need an extra check
        // Is not completely reliable (e.g. Vivo devices), see workaround on load failure
        // Note: can not use check for Android classes as testing frameworks (Robolectric)
        // may provide them on non-Android devices
        final boolean android = vendor.contains("Android");
        if (!android) {
            if (osName.contains("mac")) {
                isLinux = false;
                // Note: for macOS using universal library supporting x64 and arm64
                libname += "-macos";
                filename = "lib" + libname + ".dylib";
                checkUnpackLib(filename);
            } else {
                String cpuArchPostfix = "-" + getCpuArch();
                if (osName.contains("windows")) {
                    isLinux = false;
                    libname += "-windows" + cpuArchPostfix;
                    filename = libname + ".dll";
                    checkUnpackLib(filename);
                } else if (osName.contains("linux")) {
                    libname += "-linux" + cpuArchPostfix;
                    filename = "lib" + libname + ".so";
                    checkUnpackLib(filename);
                }
            }
        }
        try {
            File file = new File(filename);
            if (file.exists()) {
                System.load(file.getAbsolutePath());
            } else {
                try {
                    if (android) {
                        boolean success = loadLibraryAndroid();
                        if (!success) {
                            System.loadLibrary(libname);
                        }
                    } else {
                        System.err.println("File not available: " + file.getAbsolutePath());
                        System.loadLibrary(libname);
                    }
                } catch (UnsatisfiedLinkError e) {
                    if (!android && isLinux) {
                        // maybe is Android, but check failed: try loading Android lib
                        boolean success = loadLibraryAndroid();
                        if (!success) {
                            System.loadLibrary(OBJECTBOX_JNI);
                        }
                    } else {
                        throw e;
                    }
                }
            }
        } catch (UnsatisfiedLinkError e) {
            String osArch = System.getProperty("os.arch");
            String message;
            if (android) {
                message = String.format(
                        "[ObjectBox] Android failed to load native library," +
                                " check your APK/App Bundle includes a supported ABI or use ReLinker" +
                                " https://docs.objectbox.io/android/app-bundle-and-split-apk" +
                                " (vendor=%s,os=%s,os.arch=%s,SUPPORTED_ABIS=%s)",
                        vendor, osName, osArch, getSupportedABIsAndroid()
                );
            } else {
                String sunArch = System.getProperty("sun.arch.data.model");
                message = String.format(
                        "[ObjectBox] Loading native library failed, please report this to us: " +
                                "vendor=%s,os=%s,os.arch=%s,model=%s,linux=%s,machine=%s",
                        vendor, osName, osArch, sunArch, isLinux, getCpuArchOSOrNull()
                );
            }
            throw new LinkageError(message, e); // UnsatisfiedLinkError does not allow a cause; use its super class
        }
    }

    /**
     * Get CPU architecture of the JVM (Note: this can not be used for Android, Android decides arch on its own
     * and looks for library in appropriately named folder).
     * <p>
     * Note that this may not be the architecture of the actual hardware
     * (e.g. when running a x86 JVM on an amd64 machine).
     */
    private static String getCpuArch() {
        String osArch = System.getProperty("os.arch");
        String cpuArch = null;
        if (osArch != null) {
            osArch = osArch.toLowerCase();
            if (osArch.equalsIgnoreCase("amd64") || osArch.equalsIgnoreCase("x86_64")) {
                cpuArch = "x64";
            } else if (osArch.equalsIgnoreCase("x86")) {
                cpuArch = "x86";
            } else if ("aarch64".equals(osArch) || osArch.startsWith("armv8") || osArch.startsWith("arm64")) {
                // 64-bit ARM
                cpuArch = "arm64";
            } else if (osArch.startsWith("arm")) {
                // 32-bit ARM
                if (osArch.startsWith("armv7") || osArch.startsWith("armeabi-v7")) {
                    cpuArch = "armv7";
                } else if (osArch.startsWith("armv6")) {
                    cpuArch = "armv6";
                } else if ("arm".equals(osArch)) {
                    // JVM may just report "arm" for any 32-bit ARM, so try to check with OS.
                    String cpuArchOSOrNull = getCpuArchOSOrNull();
                    if (cpuArchOSOrNull != null) {
                        String cpuArchOS = cpuArchOSOrNull.toLowerCase();
                        if (cpuArchOS.startsWith("armv7")) {
                            cpuArch = "armv7";
                        } else if (cpuArchOS.startsWith("armv6")) {
                            cpuArch = "armv6";
                        } // else use fall back below.
                    } // else use fall back below.
                }
                if (cpuArch == null) {
                    // Fall back to lowest supported 32-bit ARM version.
                    cpuArch = "armv6";
                    System.err.printf("[ObjectBox] 32-bit ARM os.arch unknown (will use %s), " +
                                    "please report this to us: os.arch=%s, machine=%s%n",
                            cpuArch, osArch, getCpuArchOSOrNull());
                }
            }
        }
        // If os.arch is not covered above try a x86 version based on JVM bit-ness.
        if (cpuArch == null) {
            String sunArch = System.getProperty("sun.arch.data.model");
            if ("64".equals(sunArch)) {
                cpuArch = "x64";
            } else if ("32".equals(sunArch)) {
                cpuArch = "x86";
            } else {
                cpuArch = "unknown";
            }
            System.err.printf("[ObjectBox] os.arch unknown (will use %s), " +
                            "please report this to us: os.arch=%s, model=%s, machine=%s%n",
                    cpuArch, osArch, sunArch, getCpuArchOSOrNull());
        }
        return cpuArch;
    }

    /**
     * Get architecture using operating system tools. Currently only Linux is supported (using uname).
     */
    @Nullable
    private static String getCpuArchOSOrNull() {
        String archOrNull = null;
        try {
            // Linux
            Process exec = Runtime.getRuntime().exec("uname -m");
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(exec.getInputStream(), Charset.defaultCharset()));
            archOrNull = reader.readLine();
            reader.close();
        } catch (Exception ignored) {
        }
        return archOrNull;
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

    private static boolean loadLibraryAndroid() {
        if (BoxStore.getContext() == null) {
            return false;
        }

        //noinspection TryWithIdenticalCatches
        try {
            Class<?> context = Class.forName("android.content.Context");
            if (BoxStore.getRelinker() == null) {
                // use default ReLinker
                Class<?> relinker = Class.forName("com.getkeepsafe.relinker.ReLinker");
                Method loadLibrary = relinker.getMethod("loadLibrary", context, String.class, String.class);
                loadLibrary.invoke(null, BoxStore.getContext(), OBJECTBOX_JNI, BoxStore.JNI_VERSION);
            } else {
                // use custom ReLinkerInstance
                Method loadLibrary = BoxStore.getRelinker().getClass().getMethod("loadLibrary", context, String.class, String.class);
                loadLibrary.invoke(BoxStore.getRelinker(), BoxStore.getContext(), OBJECTBOX_JNI, BoxStore.JNI_VERSION);
            }
        } catch (NoSuchMethodException e) {
            return false;
        } catch (IllegalAccessException e) {
            return false;
        } catch (InvocationTargetException e) {
            return false;
        } catch (ClassNotFoundException e) {
            return false;
        }
        // note: do not catch Exception as it will swallow ReLinker exceptions useful for debugging
        // note: can't catch ReflectiveOperationException, is K+ (19+) on Android

        return true;
    }

    /**
     * Return a string containing a list of ABIs that are supported by the current Android device.
     * If the Android device is below API level 21 (Android 5) or if looking up the value fails,
     * returns an empty string.
     */
    @Nonnull
    private static String getSupportedABIsAndroid() {
        String[] supportedAbis = null;
        //noinspection TryWithIdenticalCatches
        try {
            Class<?> build = Class.forName("android.os.Build");
            Field supportedAbisField = build.getField("SUPPORTED_ABIS");
            supportedAbis = (String[]) supportedAbisField.get(null);
        } catch (NoSuchFieldException ignored) {
        } catch (IllegalAccessException ignored) {
        } catch (ClassNotFoundException ignored) {
        }
        // note: can't use multi-catch (would compile to ReflectiveOperationException, is K+ (19+) on Android)

        if (supportedAbis != null) {
            return Arrays.toString(supportedAbis);
        } else {
            return "";
        }
    }

    public static void ensureLoaded() {
    }
}
