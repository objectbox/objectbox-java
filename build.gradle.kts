// This script supports some Gradle project properties:
// https://docs.gradle.org/current/userguide/build_environment.html#sec:project_properties
// - versionPostFix: appended to snapshot version number, e.g. "1.2.3-<versionPostFix>-SNAPSHOT".
//   Use to create different versions based on branch/tag.
// - sonatypeUsername: Maven Central credential used by Nexus publishing.
// - sonatypePassword: Maven Central credential used by Nexus publishing.
// This script supports the following environment variables:
// - OBX_RELEASE: If set to "true" builds release versions without version postfix.
//   Otherwise, will build snapshot versions.

plugins {
    // https://github.com/ben-manes/gradle-versions-plugin/releases
    id("com.github.ben-manes.versions") version "0.51.0"
    // https://github.com/spotbugs/spotbugs-gradle-plugin/releases
    id("com.github.spotbugs") version "6.0.26" apply false
    // https://github.com/gradle-nexus/publish-plugin/releases
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
}

buildscript {
    // Version of Maven artifacts
    // Should only be changed as part of the release process, see the release checklist in the objectbox repo
    val versionNumber = "5.2.0"

    // Release mode should only be enabled when manually triggering a CI pipeline,
    // see the release checklist in the objectbox repo.
    // If true won't build snapshots and removes version post fix (e.g. "-dev-SNAPSHOT"),
    // uses release versions of dependencies.
    // val isRelease = System.getenv("OBX_RELEASE") == "true"
    val isRelease = true // On (public) main branch don't use snapshots (publishing is disabled in CI for main branch)

    // version post fix: "-<value>" or "" if not defined; e.g. used by CI to pass in branch name
    val versionPostFixValue = project.findProperty("versionPostFix")
    val versionPostFix = if (versionPostFixValue != null) "-$versionPostFixValue" else ""
    val obxJavaVersion by extra(versionNumber + (if (isRelease) "" else "$versionPostFix-SNAPSHOT"))

    // Native library version for tests
    // Be careful to diverge here; easy to forget and hard to find JNI problems
    val nativeVersion = versionNumber + (if (isRelease) "" else "-dev-SNAPSHOT")
    val osName = System.getProperty("os.name").lowercase()
    val objectboxPlatform = when {
        osName.contains("linux") -> "linux"
        osName.contains("windows") -> "windows"
        osName.contains("mac") -> "macos"
        else -> "unsupported"
    }
    val obxJniLibVersion by extra("io.objectbox:objectbox-$objectboxPlatform:$nativeVersion")

    println("version=$obxJavaVersion")
    println("objectboxNativeDependency=$obxJniLibVersion")

    // To avoid duplicate release artifacts on the internal repository,
    // prevent publishing from branches other than publish, and main (for which publishing is turned off).
    val isCI = System.getenv("CI") == "true"
    val branchOrTag = System.getenv("CI_COMMIT_REF_NAME")
    if (isCI && isRelease && !("publish" == branchOrTag || "main" == branchOrTag)) {
        throw GradleException("isRelease = true only allowed on publish or main branch, but is $branchOrTag")
    }

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

    repositories {
        mavenCentral()
        maven {
            url = uri("https://plugins.gradle.org/m2/")
        }
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
        classpath("org.jetbrains.dokka:dokka-gradle-plugin:$dokkaVersion")
    }
}

allprojects {
    group = "io.objectbox"
    val obxJavaVersion: String by rootProject.extra
    version = obxJavaVersion

    repositories {
        mavenCentral()
    }

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

            if (project.hasProperty("sonatypeUsername") && project.hasProperty("sonatypePassword")) {
                println("Publishing: Sonatype Maven Central credentials supplied.")
                username.set(project.property("sonatypeUsername").toString())
                password.set(project.property("sonatypePassword").toString())
            } else {
                println("Publishing: Sonatype Maven Central credentials NOT supplied.")
            }
        }
    }
}
