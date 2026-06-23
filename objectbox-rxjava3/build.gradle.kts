import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("java-library")
    alias(libs.plugins.kotlin.jvm)
    id("objectbox.dokka-conventions")
    id("objectbox.publishing-conventions")
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

dependencies {
    api(project(":objectbox-java"))
    api(libs.rxjava3)

    testImplementation(libs.junit)
    testImplementation(libs.mockito)
}

val sourcesJar by tasks.registering(Jar::class) {
    group = "build"
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

// Note: common settings applied by objectbox.publishing-conventions plugin
val publicationObjectboxRxjava3 = "objectboxRxjava3"
publishing {
    publications {
        create<MavenPublication>(publicationObjectboxRxjava3) {
            artifactId = "objectbox-rxjava3"

            from(components["java"])
            artifact(sourcesJar)
            // Note: the javadocJar task is created by the objectbox.dokka-conventions plugin
            artifact(tasks.named("javadocJar"))

            pom {
                name.set("ObjectBox RxJava 3 API")
                description.set("RxJava 3 extensions for ObjectBox")
                packaging = "jar"
            }
        }
    }
}

signing {
    sign(publishing.publications[publicationObjectboxRxjava3])
}
