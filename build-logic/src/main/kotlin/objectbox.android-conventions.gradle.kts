// This convention plugin applies the Android library plugin and configures settings common to all
// ObjectBox Android library projects. Projects still configure their specific settings, like
// namespace, flavors and published variants.

plugins {
    id("com.android.library")
}

android {
    // Note: increasing compile SDK also signals this library is compatible with any changes in that
    // API level, see "behavior changes" for each Android version at https://developer.android.com/about/versions
    compileSdk = 36 // Android 16 (BAKLAVA)

    // Common configuration for all variants (variant = flavor + build type).
    defaultConfig {
        minSdk = 21 // Android 5.0 (Lollipop), the minimum of NDK r27 used by the database library
    }

    // https://developer.android.com/studio/build/build-variants#build-types
    // Common configuration for release build types (debug currently left at default).
    buildTypes {
        release {
            // Currently not obfuscating/minifying with ProGuard/R8.
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }

    // Java 8 features support https://developer.android.com/studio/write/java8-support
    // Note: this requires consuming projects to also enable this.
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    testOptions {
        // Robolectric: for local unit tests enable use of Android framework
        unitTests.isIncludeAndroidResources = true
        // Robolectric: for local unit tests open internal JDK classes
        unitTests.all {
            it.jvmArgs(
                "--add-opens=java.base/java.lang=ALL-UNNAMED",
                "--add-opens=java.base/java.util=ALL-UNNAMED",
                "--add-opens=java.base/java.io=ALL-UNNAMED",
                "--add-opens=java.base/java.net=ALL-UNNAMED",
                "--add-opens=java.base/java.security=ALL-UNNAMED",
                "--add-opens=java.base/java.text=ALL-UNNAMED",
                "--add-opens=java.base/jdk.internal.access=ALL-UNNAMED",
                "--add-opens=java.desktop/java.awt.font=ALL-UNNAMED",
                "--add-opens=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
            )
        }
    }
}
