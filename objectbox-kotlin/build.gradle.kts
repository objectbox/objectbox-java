import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import java.net.URL

plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka")
    id("objectbox-publish")
}

// Note: use release flag instead of sourceCompatibility and targetCompatibility to ensure only JDK 8 API is used.
// https://docs.gradle.org/current/userguide/building_java_projects.html#sec:java_cross_compilation
tasks.withType<JavaCompile> {
    options.release.set(8)
}

kotlin {
    compilerOptions {
        // Produce Java 8 byte code, would default to Java 6
        jvmTarget.set(JvmTarget.JVM_1_8)

        // Allow consumers of this library to use the oldest possible Kotlin compiler and standard libraries.
        // https://kotlinlang.org/docs/compatibility-modes.html
        // https://kotlinlang.org/docs/kotlin-evolution-principles.html#compatibility-tools

        // Prevents using newer language features, sets this as the Kotlin version in produced metadata. So consumers
        // can compile this with a Kotlin compiler down to one minor version before this.
        // Pick the oldest not deprecated version.
        languageVersion.set(KotlinVersion.KOTLIN_1_7)
        // Prevents using newer APIs from the Kotlin standard library. So consumers can run this library with a Kotlin
        // standard library down to this version.
        // Pick the oldest not deprecated version.
        apiVersion.set(KotlinVersion.KOTLIN_1_7)
        // Depend on the oldest compatible Kotlin standard libraries (by default the Kotlin plugin coerces it to the one
        // matching its version). So consumers can safely use this or any later Kotlin standard library.
        // Pick the first release matching the versions above.
        // Note: when changing, also update coroutines dependency version (as this does not set that).
        coreLibrariesVersion = "1.7.0"
    }
}

val dokkaHtml = tasks.named<DokkaTask>("dokkaHtml")
dokkaHtml.configure {
    outputDirectory.set(layout.buildDirectory.dir("docs/javadoc"))

    dokkaSourceSets.configureEach {
        // Fix "Can't find node by signature": have to manually point to dependencies.
        // https://github.com/Kotlin/dokka/wiki/faq#dokka-complains-about-cant-find-node-by-signature-
        externalDocumentationLink {
            // Point to web javadoc for objectbox-java packages.
            url.set(URL("https://objectbox.io/docfiles/java/current/"))
            // Note: Using JDK 9+ package-list is now called element-list.
            packageListUrl.set(URL("https://objectbox.io/docfiles/java/current/element-list"))
        }
    }
}

val javadocJar by tasks.registering(Jar::class) {
    dependsOn(dokkaHtml)
    group = "build"
    archiveClassifier.set("javadoc")
    from(dokkaHtml.get().outputDirectory)
}

val sourcesJar by tasks.registering(Jar::class) {
    group = "build"
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

dependencies {
    // Note: compileOnly so consumers do not depend on the coroutines library unless they manually add it.
    // Note: pick a version that depends on Kotlin standard library (org.jetbrains.kotlin:kotlin-stdlib) version
    // coreLibrariesVersion (set above) or older.
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")

    api(project(":objectbox-java"))
}

// Set project-specific properties.
publishing {
    publications {
        getByName<MavenPublication>("mavenJava") {
            from(components["java"])
            artifact(sourcesJar)
            artifact(javadocJar)
            pom {
                name.set("ObjectBox Kotlin")
                description.set("ObjectBox is a fast NoSQL database for Objects")
            }
        }
    }
}
