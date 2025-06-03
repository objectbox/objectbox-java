import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
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

tasks.withType<KotlinCompile> {
    kotlinOptions {
        // Produce Java 8 byte code, would default to Java 6.
        jvmTarget = "1.8"
        // Allow consumers of this library to use an older version of the Kotlin compiler. By default only the version
        // previous to the compiler used for this project typically works.
        // Kotlin supports the development with at least three previous versions, so pick the oldest one possible.
        // https://kotlinlang.org/docs/kotlin-evolution.html#evolving-the-binary-format
        // https://kotlinlang.org/docs/compatibility-modes.html
        apiVersion = "1.7"
        languageVersion = "1.7"
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

val coroutinesVersion: String by rootProject.extra

dependencies {
    // Note: compileOnly as we do not want to require library users to use coroutines.
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")

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
