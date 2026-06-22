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
    compileSdk = 35 // Android 15 (Vanilla Ice Cream)

    defaultConfig {
        minSdk = 21 // Android 5.0 (Lollipop), like objectbox-android

        consumerProguardFiles("consumer-proguard-rules.pro")

        // Dependencies: objectbox-android also has a "database" dimension,
        // always pick its "sync" flavor.
        missingDimensionStrategy("database", "sync")
    }

    buildTypes {
        release {
            // Currently not obfuscating/minifying with ProGuard/R8.
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"))
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

dependencies {
    // Use "api" to add the Java library as a "compile" dependency in the POM as this library
    // exposes APIs from it (MeshConfig). Regardless, it is expected that most consumers (or the
    // Gradle plugin) add the Java library as a direct dependency.
    api(project(":objectbox-java"))
    // Use "implementation" to add the Nearby Connections library as a "runtime" dependency in the
    // POM for consumers (it is required at runtime), but not expose any of its types via this
    // library's API.
    implementation(libs.play.services.nearby)

    // With Gradle, it is currently not possible to select between Maven coordinates of multiple
    // publications (https://github.com/gradle/gradle/issues/12324) of objectbox-android that this
    // projects variants depend on (Sync with Admin and without Admin).
    // As a workaround, manually modify the POM XML for each publication (see publications block) to
    // add the correct dependency. And instead of "api" use "compileOnly" here to avoid Gradle
    // adding the dependency to the POM, but still allow code in this project to use APIs from
    // objectbox-android.
    compileOnly(project(":objectbox-android"))

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

                // Workaround to depend on specific publication of subproject objectbox-android, see
                // notes in dependencies block above.
                // The groupId, artifactId and version must match with that of a publication of
                // objectbox-android.
                addCompileDependency(groupId, "objectbox-sync-android", version)
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

                // Workaround to depend on specific publication of subproject objectbox-android, see
                // notes in dependencies block above.
                // The groupId, artifactId and version must match with that of a publication of
                // objectbox-android.
                addCompileDependency(groupId, "objectbox-sync-android-objectbrowser", version)
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

/**
 * Manually adds a "compile" dependency to the POM.
 *
 * Note that this is a workaround and typically dependencies should be added via configurations.
 */
private fun MavenPom.addCompileDependency(groupId: String, artifactId: String, version: String) {
    withXml {
        val root = this.asElement()
        val document = root.ownerDocument
        val dependenciesNode = checkNotNull(
            root.getElementsByTagName("dependencies").item(0)
        ) {
            "Expected existing <dependencies> node in generated POM"
        }
        val dependencyNode = dependenciesNode.appendChild(document.createElement("dependency"))
        dependencyNode.appendChild(document.createElement("groupId"))
            .textContent = groupId
        dependencyNode.appendChild(document.createElement("artifactId"))
            .textContent = artifactId
        dependencyNode.appendChild(document.createElement("version"))
            .textContent = version
        dependencyNode.appendChild(document.createElement("scope"))
            .textContent = "compile"
    }
}

signing {
    sign(publishing.publications[publicationMeshSyncAndroid])
    sign(publishing.publications[publicationMeshSyncAndroidAdmin])
}
