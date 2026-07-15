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
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.dokka) apply false
    alias(libs.plugins.versions)
    alias(libs.plugins.spotbugs) apply false
    alias(libs.plugins.publish)
    alias(libs.plugins.android.library) apply false
}

buildscript {
    // Environment variables (see notes at the top of this file)
    // https://docs.gitlab.com/ci/variables/predefined_variables/
    val envIsCI: Boolean = System.getenv("CI") == "true"
    val envRelease: String? = System.getenv("OBX_RELEASE")
    // Gradle properties (see notes at the top of this file)
    val propertyVersionSuffixName = "versionSuffix"
    val propertyVersionSuffix = providers.gradleProperty(propertyVersionSuffixName)

    // Version of Maven artifacts
    // Should only be changed as part of the release process, see the release checklist in the objectbox repo
    val versionNumber = "6.0.0"

    // If OBX_RELEASE is set, build and depend on release versions. Doesn't publish a release.
    // See the release checklist in the objectbox repo on how to publish a release.
    // If true, Maven artifacts use a release version, so without branch name and snapshot suffix
    // (such as "-dev-SNAPSHOT"), including for dependencies (such as objectbox-java).
    val isRelease = envRelease == "true"

    if (!isRelease && envIsCI) {
        throw GradleException("Publishing: property $propertyVersionSuffixName must be set in CI to calculate version suffix.")
    }

    // version suffix: "-<value>" or "" if not defined; e.g. used by CI to pass in branch name
    val versionSuffix = if (propertyVersionSuffix.isPresent) "-${propertyVersionSuffix.get()}" else ""
    val obxJavaVersion by extra(versionNumber + (if (isRelease) "" else "$versionSuffix-SNAPSHOT"))
    println("Publishing: version = $obxJavaVersion")

    // JVM and Android database library versions
    val versionDbJvm = if (isRelease) versionNumber else "$versionNumber-dev-SNAPSHOT"
    val versionDbJvmSync = if (isRelease) versionNumber else "$versionNumber-sync-SNAPSHOT"
    val versionDbAndroid = if (isRelease) versionNumber else "$versionNumber-dev-SNAPSHOT"
    val versionDbAndroidSync = if (isRelease) versionNumber else "$versionNumber-sync-SNAPSHOT"

    println("Database dependencies (JVM) = $versionDbJvm")
    println("Database dependencies (JVM + Sync) = $versionDbJvmSync")
    println("Database dependencies (Android) = $versionDbAndroid")
    println("Database dependencies (Android + Sync) = $versionDbAndroidSync")

    val versionDatabaseLibraryJvm by extra(versionDbJvm)
    val versionDatabaseLibraryJvmSync by extra(versionDbJvmSync)
    val versionDatabaseLibraryAndroid by extra(versionDbAndroid)
    val versionDatabaseLibraryAndroidSync by extra(versionDbAndroidSync)
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
