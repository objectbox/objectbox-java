plugins {
    alias(libs.plugins.android.library)
    id("maven-publish")
    id("signing")
}

android {
    namespace = "io.objectbox.android"
    compileSdk = 35 // Android 15 (Vanilla Ice Cream)

    // Common configuration for all variants (variant = flavor + build type).
    defaultConfig {
        minSdk = 21 // Android 5.0 (Lollipop), minimum of NDK r27 is 21
        // For Android libraries use target SDK to indicate the latest tested/supported version.
        targetSdk = 33 // Android 13 (Tiramisu); update to 34+ is blocked by objectbox-java#226

        consumerProguardFiles("consumer-proguard-rules.pro")
    }

    signingConfigs {
        getByName("debug") {
            storeFile = file(System.getProperty("user.home") + "/.android/debug.keystore")
        }
    }

    // https://developer.android.com/studio/build/build-variants#build-types
    // Common configuration for release build types (debug currently left at default).
    buildTypes {
        release {
            // Currently not obfuscating/minifying with ProGuard/R8.
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
            // Just to use without checkjni
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    // https://developer.android.com/studio/build/build-variants#product-flavors
    // Common configuration for flavors. Dimensions are used to enable a matrix configuration.
    // Note: common configuration defined in defaultConfig and buildTypes blocks above.
    // Note: depending on if the Admin feature is enabled different (re)sources are included
    // (see src/dbOnly and src/dbAndAdmin which must be named like the flavor).
    flavorDimensions += listOf("sources")
    productFlavors {
        create("dbOnly") {
            dimension = "sources"
        }
        create("dbAndAdmin") {
            dimension = "sources"
        }
    }

    // https://developer.android.com/studio/publish-library/configure-pub-variants
    // Pick variants (variant = flavor + build type) to publish.
    publishing {
        singleVariant("dbOnlyRelease") {
            withJavadocJar()
            withSourcesJar()
        }
        singleVariant("dbAndAdminRelease") {
            withJavadocJar()
            withSourcesJar()
        }
    }

    // Java 8 features support https://developer.android.com/studio/write/java8-support
    // Note: this requires consuming projects to also enable this.
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    // For local unit tests enable use of Android framework with Robolectric
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    api(project(":objectbox-java"))

    compileOnly("androidx.lifecycle:lifecycle-livedata:2.6.1")
    // Note: Paging v3 requires Kotlin.
    compileOnly("androidx.paging:paging-runtime:2.1.2")

    // Sync: add dependencies for local unit tests.
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("androidx.test:rules:1.5.0")
    testImplementation("androidx.test.ext:junit:1.1.5")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.10.3")
}

// FIXME
//publishing {
//    addObxGitLabRepoIfConfigured(project)
//
//    publications {
//        // Sync: add -sync to artifact name, use Sync components.
//        create<MavenPublication>("mavenJava") {
//            artifactId = "objectbox-sync-android"
//
//            // Because the Android components are created during the evaluation phase,
//            // can only use them in the afterEvaluate() lifecycle method.
//            afterEvaluate {
//                from(components["dbOnlySyncClientRelease"])
//            }
//
//            pom {
//                name.set("ObjectBox Android")
//                packaging = "aar"
//
//                setCommonObxProperties()
//                setObxNativeLicenses()
//            }
//        }
//        // Sync: add -sync to artifact name, use Sync components.
//        create<MavenPublication>("mavenJavaBrowser") {
//            artifactId = "objectbox-sync-android-objectbrowser"
//
//            // Because the Android components are created during the evaluation phase,
//            // can only use them in the afterEvaluate() lifecycle method.
//            afterEvaluate {
//                from(components["dbAndAdminSyncClientAndAdminRelease"])
//            }
//
//            // Note: as publication name is different POM properties are not inherited.
//            pom {
//                name.set("ObjectBox Android with Admin")
//                packaging = "aar"
//
//                setCommonObxProperties()
//                setObxNativeLicenses()
//            }
//        }
//        // Sync: add server publication.
//        if (buildSyncServer) {
//            create<MavenPublication>("mavenJavaServer") {
//                artifactId = "objectbox-sync-server-android"
//
//                // Because the Android components are created during the evaluation phase,
//                // can only use them in the afterEvaluate() lifecycle method.
//                afterEvaluate {
//                    from(components["dbAndAdminSyncServerRelease"])
//                }
//
//                pom {
//                    name.set("ObjectBox Android with Sync Server")
//                    packaging = "aar"
//
//                    setCommonObxProperties()
//                    setObxNativeLicenses()
//                }
//            }
//        }
//    }
//}
//
//signing {
//    signIfConfigured(project) {
//        sign(publishing.publications["mavenJava"])
//        sign(publishing.publications["mavenJavaBrowser"])
//        // Sync: sign server publication.
//        if (buildSyncServer) {
//            sign(publishing.publications["mavenJavaServer"])
//        }
//    }
//}
