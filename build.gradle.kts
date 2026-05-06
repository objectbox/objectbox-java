/*
 * This script supports some Gradle project properties:
 *
 * - versionSuffix: appended to snapshot version number, e.g. "1.2.3-<versionSuffix>-SNAPSHOT".
 * Use to create different versions based on branch/tag.
 * - sonatypeUsername: Maven Central credential used by Nexus publishing.
 * - sonatypePassword: Maven Central credential used by Nexus publishing.
 *
 * This script supports the following environment variables:
 *
 * - OBX_RELEASE: If set to "true" builds and depends on release versions, without branch name and snapshot suffix.
 */

// Gradle properties (more defined in buildscript block below)
val propertySonatypeUsername = providers.gradleProperty("sonatypeUsername")
val propertySonatypePassword = providers.gradleProperty("sonatypePassword")

plugins {
    // https://github.com/ben-manes/gradle-versions-plugin/releases
    id("com.github.ben-manes.versions") version "0.51.0"
    // https://github.com/spotbugs/spotbugs-gradle-plugin/releases
    id("com.github.spotbugs") version "6.0.26" apply false
    // https://github.com/gradle-nexus/publish-plugin/releases
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
    alias(libs.plugins.android.library) apply false
}

buildscript {
    // Environment variables (see notes at the top of this file)
    val envRelease: String? = System.getenv("OBX_RELEASE")
    // Gradle properties (see notes at the top of this file)
    val propertyVersionSuffix = providers.gradleProperty("versionSuffix")

    // Version of Maven artifacts
    // Should only be changed as part of the release process, see the release checklist in the objectbox repo
    val versionNumber = "5.4.2"

    // If OBX_RELEASE is set, build and depend on release versions. Doesn't publish a release.
    // See the release checklist in the objectbox repo on how to publish a release.
    // If true, Maven artifacts use a release version, so without branch name and snapshot suffix
    // (such as "-dev-SNAPSHOT"), including for dependencies (such as objectbox-java).
    val isRelease = envRelease == "true"

    // version suffix: "-<value>" or "" if not defined; e.g. used by CI to pass in branch name
    val versionSuffix = if (propertyVersionSuffix.isPresent) "-${propertyVersionSuffix.get()}" else ""
    val obxJavaVersion by extra(versionNumber + (if (isRelease) "" else "$versionSuffix-SNAPSHOT"))
    println("Publishing: version = $obxJavaVersion")

    // JVM and Android database library versions
    val versionDbJvm = if (isRelease) versionNumber else "$versionNumber-dev-SNAPSHOT"
    val versionDbAndroid = if (isRelease) versionNumber else "$versionNumber-dev-SNAPSHOT"
    val versionDbAndroidSync = if (isRelease) versionNumber else "$versionNumber-sync-SNAPSHOT"

    println("Database dependencies (JVM) = $versionDbJvm")
    println("Database dependencies (Android) = $versionDbAndroid")
    println("Database dependencies (Android + Sync) = $versionDbAndroidSync")

    val versionDatabaseLibraryJvm by extra(versionDbJvm)
    val versionDatabaseLibraryAndroid by extra(versionDbAndroid)
    val versionDatabaseLibraryAndroidSync by extra(versionDbAndroidSync)

    // Versions for third party dependencies and plugins
    val essentialsVersion by extra("3.1.0")
    val junitVersion by extra("4.13.2")
    val mockitoVersion by extra("3.8.0")
    // The versions of Gradle, Kotlin and Kotlin Coroutines must work together.
    // Check
    // - https://kotlinlang.org/docs/gradle-configure-project.html#apply-the-plugin
    // - https://github.com/Kotlin/kotlinx.coroutines#readme
    // Note: when updating to a new minor version also have to increase the minimum compiler and standard library
    // version supported by consuming projects, see objectbox-kotlin/ build script.
    val kotlinVersion by extra("2.0.21")
    val coroutinesVersion by extra("1.9.0")
    // Dokka includes its own version of the Kotlin compiler, so it must not match the used Kotlin version.
    // But it might not understand new language features.
    val dokkaVersion by extra("1.9.20")

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
        classpath("org.jetbrains.dokka:dokka-gradle-plugin:$dokkaVersion")
    }
}

allprojects {
    group = "io.objectbox"
    val obxJavaVersion: String by rootProject.extra
    version = obxJavaVersion

    configurations.all {
        // Projects are using snapshot dependencies that may update more often than 24 hours.
        resolutionStrategy {
            cacheChangingModulesFor(0, "seconds")
        }
    }

    tasks.withType<Javadoc>().configureEach {
        // To support Unicode characters in API docs force the javadoc tool to use UTF-8 encoding.
        // Otherwise, it defaults to the system file encoding. This is required even though setting file.encoding
        // for the Gradle daemon (see gradle.properties) as Gradle does not pass it on to the javadoc tool.
        options.encoding = "UTF-8"
    }
}

// Exclude pre-release versions from dependencyUpdates task
fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}
tasks.withType<com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask> {
    rejectVersionIf {
        isNonStable(candidate.version)
    }
}

tasks.wrapper {
    distributionType = Wrapper.DistributionType.ALL
}

// Plugin to publish to Maven Central https://github.com/gradle-nexus/publish-plugin/
// This plugin ensures a separate, named staging repo is created for each build when publishing.
nexusPublishing {
    this.repositories {
        sonatype {
            // Use the Portal OSSRH Staging API as this plugin does not support the new Portal API
            // https://central.sonatype.org/publish/publish-portal-ossrh-staging-api/#configuring-your-plugin
            nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
            snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))

            if (propertySonatypeUsername.isPresent && propertySonatypePassword.isPresent) {
                println("Publishing: Maven Central credentials supplied")
                username.set(propertySonatypeUsername.get())
                password.set(propertySonatypePassword.get())
            } else {
                println("Publishing: Maven Central credentials NOT supplied, see root build script for required project properties")
            }
        }
    }
}
