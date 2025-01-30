// This script supports some Gradle project properties:
// https://docs.gradle.org/current/userguide/build_environment.html#sec:project_properties
// - versionPostFix: appended to snapshot version number, e.g. "1.2.3-<versionPostFix>-SNAPSHOT".
//   Use to create different versions based on branch/tag.
// - sonatypeUsername: Maven Central credential used by Nexus publishing.
// - sonatypePassword: Maven Central credential used by Nexus publishing.

plugins {
    // https://github.com/spotbugs/spotbugs-gradle-plugin/releases
    id("com.github.spotbugs") version "5.0.14" apply false
    // https://github.com/gradle-nexus/publish-plugin/releases
    id("io.github.gradle-nexus.publish-plugin") version "1.3.0"
}

buildscript {
    // To publish a release, typically, only edit those two:
    val objectboxVersionNumber = "4.1.0" // without "-SNAPSHOT", e.g. "2.5.0" or "2.4.0-RC"
    val objectboxVersionRelease =
        true // set to true for releasing to ignore versionPostFix to avoid e.g. "-dev" versions

    // version post fix: "-<value>" or "" if not defined; e.g. used by CI to pass in branch name
    val versionPostFixValue = project.findProperty("versionPostFix")
    val versionPostFix = if (versionPostFixValue != null) "-$versionPostFixValue" else ""
    val obxJavaVersion by extra(objectboxVersionNumber + (if (objectboxVersionRelease) "" else "$versionPostFix-SNAPSHOT"))

    // Native library version for tests
    // Be careful to diverge here; easy to forget and hard to find JNI problems
    val nativeVersion = objectboxVersionNumber + (if (objectboxVersionRelease) "" else "-dev-SNAPSHOT")
    val osName = System.getProperty("os.name").lowercase()
    val objectboxPlatform = when {
        osName.contains("linux") -> "linux"
        osName.contains("windows") -> "windows"
        osName.contains("mac") -> "macos"
        else -> "unsupported"
    }
    val obxJniLibVersion by extra("io.objectbox:objectbox-$objectboxPlatform:$nativeVersion")

    val essentialsVersion by extra("3.1.0")
    val junitVersion by extra("4.13.2")
    val mockitoVersion by extra("3.8.0")
    // The versions of Kotlin, Kotlin Coroutines and Dokka must work together.
    // Check https://github.com/Kotlin/kotlinx.coroutines#readme
    // and https://github.com/Kotlin/dokka/releases
    // Note: when updating might also have to increase the minimum compiler version supported
    // by consuming projects, see objectbox-kotlin/ build script.
    val kotlinVersion by extra("1.8.20")
    val coroutinesVersion by extra("1.7.3")
    val dokkaVersion by extra("1.8.20")

    println("version=$obxJavaVersion")
    println("objectboxNativeDependency=$obxJniLibVersion")

    // To avoid duplicate release artifacts on the internal repository,
    // prevent uploading from branches other than publish, and main (for which uploading is turned off).
    val isCI = System.getenv("CI") == "true"
    val branchOrTag = System.getenv("CI_COMMIT_REF_NAME")
    if (isCI && objectboxVersionRelease && !("publish" == branchOrTag || "main" == branchOrTag)) {
        throw GradleException("objectboxVersionRelease = true is only allowed on branch publish or main")
    }

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
}

tasks.wrapper {
    distributionType = Wrapper.DistributionType.ALL
}

// Plugin to publish to Central https://github.com/gradle-nexus/publish-plugin/
// This plugin ensures a separate, named staging repo is created for each build when publishing.
apply(plugin = "io.github.gradle-nexus.publish-plugin")
configure<io.github.gradlenexus.publishplugin.NexusPublishExtension> {
    this.repositories {
        sonatype {
            if (project.hasProperty("sonatypeUsername") && project.hasProperty("sonatypePassword")) {
                username.set(project.property("sonatypeUsername").toString())
                password.set(project.property("sonatypePassword").toString())
                println("Publishing: configured Maven Central repository")
            } else {
                println("Publishing: Maven Central repository not configured")
            }
        }
    }
    transitionCheckOptions {  // Maven Central may become very, very slow in extreme situations
        maxRetries.set(900)  // with default delay of 10s, that's 150 minutes total; default is 60 (10 minutes)
    }
}
