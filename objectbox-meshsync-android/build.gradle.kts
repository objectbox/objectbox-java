import org.gradle.kotlin.dsl.support.uppercaseFirstChar

plugins {
    alias(libs.plugins.android.library)
    id("objectbox.publishing-conventions")
}

val flavorAdminExcluded = "adminExcluded"
val flavorAdminIncluded = "adminIncluded"
// Note: build variant names also match names of created components
val buildTypeRelease = "release"
val variantAdminExcludedRelease = "${flavorAdminExcluded}${buildTypeRelease.uppercaseFirstChar()}"
val variantAdminIncludedRelease = "${flavorAdminIncluded}${buildTypeRelease.uppercaseFirstChar()}"

android {
    namespace = "io.objectbox.meshsync.android"
    // Note: increasing compile SDK also signals this library is compatible with any changes in that
    // API level, see "behavior changes" for each Android version at https://developer.android.com/about/versions
    compileSdk = 35 // Android 15 (Vanilla Ice Cream)

    // Not using Kotlin source code, so prevent the Kotlin standard library from getting added,
    // avoid Kotlin compiler task run.
    enableKotlin = false

    defaultConfig {
        minSdk = 21 // Android 5.0 (Lollipop), like objectbox-android

        // Dependencies: objectbox-android also has a "database" dimension,
        // always pick its "sync" flavor.
        missingDimensionStrategy("database", "sync")
    }

    buildTypes {
        release {
            // Currently not obfuscating/minifying with ProGuard/R8.
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }

    // Configure a flavor dimension based on if Admin is included in the Android database library.
    // Note: common configuration defined in defaultConfig and buildTypes blocks above.
    // https://developer.android.com/studio/build/build-variants#product-flavors
    // Note: the name of this flavor should match the one of the depended on objectbox-android
    // subproject, otherwise, additonal configuration is necessary to depend on the correct variant
    // of it.
    val dimensionAdmin = "admin"
    flavorDimensions += listOf(dimensionAdmin)
    productFlavors {
        create(flavorAdminExcluded) {
            dimension = dimensionAdmin
        }
        create(flavorAdminIncluded) {
            dimension = dimensionAdmin
        }
    }

    // Publish the release variants (variant = flavor combination + build type)
    // https://developer.android.com/studio/publish-library/configure-pub-variants
    publishing {
        singleVariant(variantAdminExcludedRelease) {
            withJavadocJar()
            withSourcesJar()
        }
        singleVariant(variantAdminIncludedRelease) {
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

val versionDatabaseLibraryJvm: String by rootProject.extra
val versionDatabaseLibraryAndroidSync: String by rootProject.extra

val adminExcludedImplementation by configurations.getting
val adminIncludedImplementation by configurations.getting

dependencies {
    // Use "api" to add the Java library as a "compile" dependency in the POM as this library
    // exposes APIs from it (MeshConfig). Regardless, it is expected that most consumers (or the
    // Gradle plugin) add the Java library as a direct dependency.
    api(project(":objectbox-java"))
    // Use "implementation" to add the Nearby Connections library as a "runtime" dependency in the
    // POM for consumers (it is required at runtime), but not expose any of its types via this
    // library's API.
    implementation(libs.play.services.nearby)

    // Use "implementation" to add the Android database library as a "runtime" dependency in the
    // POM for consumers as it is required at runtime, but doesn't expose any APIs.
    // Note: as the artifacts produced by this project are also used by the ObjectBox Flutter
    // package, don't add a dependency on objectbox-android to make it easier to release for Flutter
    // only (and to avoid Flutter projects pulling in unused code and resources, such as for Admin).
    adminExcludedImplementation("io.objectbox:objectbox-sync-android-db:$versionDatabaseLibraryAndroidSync")
    adminIncludedImplementation("io.objectbox:objectbox-sync-android-db-admin:$versionDatabaseLibraryAndroidSync")

    // Dependencies for unit tests running on the JVM (so not on an Android device/emulator)
    testImplementation(libs.junit)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.robolectric)
    testImplementation("io.objectbox:objectbox-linux:${versionDatabaseLibraryJvm}")
    testImplementation("io.objectbox:objectbox-macos:${versionDatabaseLibraryJvm}")
    testImplementation("io.objectbox:objectbox-windows:${versionDatabaseLibraryJvm}")
}

// Note: common settings applied by objectbox.publishing-conventions plugin
val publicationMeshSyncAndroid = "objectboxMeshSyncAndroid"
val publicationMeshSyncAndroidAdmin = "objectboxMeshSyncAndroidAdmin"
publishing {
    publications {
        create<MavenPublication>(publicationMeshSyncAndroid) {
            artifactId = "objectbox-meshsync-android"

            // Because the Android components are created during the evaluation phase,
            // can only use them in the afterEvaluate() lifecycle method.
            afterEvaluate {
                from(components[variantAdminExcludedRelease])
            }

            pom {
                name.set("ObjectBox Mesh Sync for Android")
            }
        }
        create<MavenPublication>(publicationMeshSyncAndroidAdmin) {
            artifactId = "objectbox-meshsync-android-admin"

            // Because the Android components are created during the evaluation phase,
            // can only use them in the afterEvaluate() lifecycle method.
            afterEvaluate {
                from(components[variantAdminIncludedRelease])
            }

            pom {
                name.set("ObjectBox Mesh Sync with Admin for Android")
            }
        }
        // Additional common configuration for all Maven publications
        withType<MavenPublication> {
            pom {
                description.set("Peer-to-peer mesh sync for ObjectBox Sync on Android using Google Nearby Connections")
                packaging = "aar"
            }
        }
    }
}

signing {
    sign(publishing.publications[publicationMeshSyncAndroid])
    sign(publishing.publications[publicationMeshSyncAndroidAdmin])
}
