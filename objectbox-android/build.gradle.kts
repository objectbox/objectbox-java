import org.gradle.kotlin.dsl.support.uppercaseFirstChar


plugins {
    alias(libs.plugins.android.library)
    id("objectbox.publishing-conventions")
}

val flavorAdminExcluded = "adminExcluded"
val flavorAdminIncluded = "adminIncluded"
val flavorBasic = "basic"
val flavorSync = "sync"
// Note: build variant names also match names of created components
val variantAdminExcludedBasicRelease = "${flavorAdminExcluded}${flavorBasic.uppercaseFirstChar()}Release"
val variantAdminIncludedBasicRelease = "${flavorAdminIncluded}${flavorBasic.uppercaseFirstChar()}Release"
val variantAdminExcludedSyncRelease = "${flavorAdminExcluded}${flavorSync.uppercaseFirstChar()}Release"
val variantAdminIncludedSyncRelease = "${flavorAdminIncluded}${flavorSync.uppercaseFirstChar()}Release"

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

    // Configure a flavor dimension based on if Admin is included in the Android database library.
    // The Admin included flavor adds additional source files, resources and a different Android
    // manifest to help using Admin. The excluded flavor adds source files with no-op Admin APIs.
    // See the directories in "src/" named like the flavors.
    // Configure another flavor dimension purely to add a regular or a Sync variant of the Android
    // database library dependency. See the dependencies block.
    // Note: common configuration defined in defaultConfig and buildTypes blocks above.
    // https://developer.android.com/studio/build/build-variants#product-flavors
    val dimensionAdmin = "admin"
    val dimensionDatabase = "database"
    flavorDimensions += listOf(dimensionAdmin, dimensionDatabase)
    productFlavors {
        create(flavorAdminExcluded) {
            dimension = dimensionAdmin
        }
        create(flavorAdminIncluded) {
            dimension = dimensionAdmin
            // Set Admin included flavor as default in Android Studio as it's most likely to display
            // errors on breaking changes to the Java API dependency.
            isDefault = true
        }
        create(flavorBasic) {
            dimension = dimensionDatabase
        }
        create(flavorSync) {
            dimension = dimensionDatabase
        }
    }

    // Publish the release variants (variant = flavor combination + build type)
    // https://developer.android.com/studio/publish-library/configure-pub-variants
    publishing {
        singleVariant(variantAdminExcludedBasicRelease) {
            withJavadocJar()
            withSourcesJar()
        }
        singleVariant(variantAdminIncludedBasicRelease) {
            withJavadocJar()
            withSourcesJar()
        }
        singleVariant(variantAdminExcludedSyncRelease) {
            withJavadocJar()
            withSourcesJar()
        }
        singleVariant(variantAdminIncludedSyncRelease) {
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

// Must manually create configurations for flavor combinations to add dependencies
// (they are still used automatically by the Android plugin).
// https://developer.android.com/build/dependencies#configure_dependencies_for_a_specific_build_variant
val adminExcludedBasicImplementation by configurations.creating
val adminIncludedBasicImplementation by configurations.creating
val adminExcludedSyncImplementation by configurations.creating
val adminIncludedSyncImplementation by configurations.creating

dependencies {
    // Use "api" to add the Java library as a "compile" dependency in the POM as this library
    // exposes APIs from it (such as Scheduler in AndroidScheduler, BoxStore in Admin). Regardless,
    // it is expected that most consumers (or the Gradle plugin) add the Java library as a direct
    // dependency.
    api(project(":objectbox-java"))
    // Use "implementation" to add the database library as a "runtime" dependency in the POM as it
    // has no Java APIs to expose.
    // TODO Pull versions up to root build script
    val objectboxAndroidDbVersion = "5.4.2-android-db-only-artifact-SNAPSHOT"
    adminExcludedBasicImplementation("io.objectbox:objectbox-android-db:$objectboxAndroidDbVersion")
    adminIncludedBasicImplementation("io.objectbox:objectbox-android-db-admin:$objectboxAndroidDbVersion")
    val objectboxAndroidDbSyncVersion = "5.4.2-android-db-only-artifact-sync-SNAPSHOT"
    adminExcludedSyncImplementation("io.objectbox:objectbox-sync-android-db:$objectboxAndroidDbSyncVersion")
    adminIncludedSyncImplementation("io.objectbox:objectbox-sync-android-db-admin:$objectboxAndroidDbSyncVersion")

    // Use "compileOnly" to not add these as dependencies in the POM to avoid consumer projects
    // pulling in unused dependencies. It is expected that consumers did already add these
    // dependencies when using related classes of this library.
    compileOnly(libs.androidx.lifecycle.livedata)
    compileOnly(libs.androidx.paging.runtime)

    // Dependencies for unit tests running on the JVM (so not on an Android device/emulator)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.rules)
    testImplementation(libs.junit)
    testImplementation(libs.androidx.test.junit)
    testImplementation(libs.robolectric)
}

// Note: common settings applied by objectbox.publishing-conventions plugin
val publicationObjectboxAndroid = "objectboxAndroid"
val publicationObjectboxAndroidObjectbrowser = "objectboxAndroidObjectbrowser"
val publicationObjectboxSyncAndroid = "objectboxSyncAndroid"
val publicationObjectboxSyncAndroidObjectbrowser = "objectboxSyncAndroidObjectbrowser"
publishing {
    publications {
        create<MavenPublication>(publicationObjectboxAndroid) {
            artifactId = "objectbox-android"

            // Because the Android components are created during the evaluation phase,
            // can only use them in the afterEvaluate() lifecycle method.
            afterEvaluate {
                from(components[variantAdminExcludedBasicRelease])
            }

            pom {
                name.set("ObjectBox Android")
            }
        }
        create<MavenPublication>(publicationObjectboxAndroidObjectbrowser) {
            artifactId = "objectbox-android-objectbrowser"

            // Because the Android components are created during the evaluation phase,
            // can only use them in the afterEvaluate() lifecycle method.
            afterEvaluate {
                from(components[variantAdminIncludedBasicRelease])
            }

            pom {
                name.set("ObjectBox Android with Admin")
            }
        }
        create<MavenPublication>(publicationObjectboxSyncAndroid) {
            artifactId = "objectbox-sync-android"

            // Because the Android components are created during the evaluation phase,
            // can only use them in the afterEvaluate() lifecycle method.
            afterEvaluate {
                from(components[variantAdminExcludedSyncRelease])
            }

            pom {
                name.set("ObjectBox Android with Sync")
            }
        }
        create<MavenPublication>(publicationObjectboxSyncAndroidObjectbrowser) {
            artifactId = "objectbox-sync-android-objectbrowser"

            // Because the Android components are created during the evaluation phase,
            // can only use them in the afterEvaluate() lifecycle method.
            afterEvaluate {
                from(components[variantAdminIncludedSyncRelease])
            }

            pom {
                name.set("ObjectBox Android with Admin and Sync")
            }
        }
        // Additional common configuration for all Maven publications
        withType<MavenPublication> {
            pom {
                description.set("ObjectBox is a fast NoSQL database for Objects")
                packaging = "aar"
            }
        }
    }
}

signing {
    sign(publishing.publications[publicationObjectboxAndroid])
    sign(publishing.publications[publicationObjectboxAndroidObjectbrowser])
    sign(publishing.publications[publicationObjectboxSyncAndroid])
    sign(publishing.publications[publicationObjectboxSyncAndroidObjectbrowser])
}
