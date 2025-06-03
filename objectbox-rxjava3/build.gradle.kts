import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.net.URL

plugins {
    id("java-library")
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

val junitVersion: String by rootProject.extra
val mockitoVersion: String by rootProject.extra

dependencies {
    api(project(":objectbox-java"))
    api("io.reactivex.rxjava3:rxjava:3.0.11")

    testImplementation("junit:junit:$junitVersion")
    testImplementation("org.mockito:mockito-core:$mockitoVersion")
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

// Set project-specific properties.
publishing {
    publications {
        getByName<MavenPublication>("mavenJava") {
            from(components["java"])
            artifact(sourcesJar)
            artifact(javadocJar)
            pom {
                name.set("ObjectBox RxJava 3 API")
                description.set("RxJava 3 extensions for ObjectBox")
            }
        }
    }
}
