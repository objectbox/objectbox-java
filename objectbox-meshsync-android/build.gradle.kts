plugins {
    alias(libs.plugins.android.library)
    id("objectbox.publishing-conventions")
}

android {
    namespace = "io.objectbox.meshsync.android"
    compileSdk = 35 // Android 15 (Vanilla Ice Cream)

    defaultConfig {
        minSdk = 21 // Android 5.0 (Lollipop), like objectbox-android

        consumerProguardFiles("consumer-proguard-rules.pro")
    }

    buildTypes {
        release {
            // Currently not obfuscating/minifying with ProGuard/R8.
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"))
        }
    }

    // Publish the release variant (note: unlike objectbox-android, this library has a single
    // variant to avoid multiplying the variant matrix; it contains no native or Admin code).
    // https://developer.android.com/studio/publish-library/configure-pub-variants
    publishing {
        singleVariant("release") {
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

dependencies {
    // Use "api" to add the Java library as a "compile" dependency in the POM as this library
    // exposes APIs from it (MeshConfig). Regardless, it is expected that most consumers (or the
    // Gradle plugin) add the Java library as a direct dependency.
    api(project(":objectbox-java"))
    // Use "implementation" to bundle the Nearby Connections dependency for consumers (it is
    // required at runtime), but not expose any of its types via this library's API.
    implementation(libs.play.services.nearby)

    // Note: this library does not depend on an ObjectBox Android database library; consumers must
    // use the Sync variant (e.g. objectbox-sync-android) which includes the native mesh sync code.

    // Dependencies for unit tests running on the JVM (so not on an Android device/emulator)
    testImplementation(libs.junit)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.robolectric)
    testImplementation("io.objectbox:objectbox-linux:$versionDatabaseLibraryJvm")
}

// Note: common settings applied by objectbox.publishing-conventions plugin
val publicationMeshSyncAndroid = "objectboxMeshSyncAndroid"
publishing {
    publications {
        create<MavenPublication>(publicationMeshSyncAndroid) {
            artifactId = "objectbox-meshsync-android"

            // Because the Android components are created during the evaluation phase,
            // can only use them in the afterEvaluate() lifecycle method.
            afterEvaluate {
                from(components["release"])
            }

            pom {
                name.set("ObjectBox Mesh Sync for Android")
                description.set("Peer-to-peer mesh sync for ObjectBox Sync on Android using Google Nearby Connections")
                packaging = "aar"
            }
        }
    }
}

signing {
    sign(publishing.publications[publicationMeshSyncAndroid])
}
