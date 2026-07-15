
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
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

        // Allow consumers of this library to use the oldest possible Kotlin compiler and standard libraries.
        // https://kotlinlang.org/docs/compatibility-modes.html
        // https://kotlinlang.org/docs/kotlin-evolution-principles.html#compatibility-tools

        // Prevents using newer language features, sets this as the Kotlin version in produced metadata. So consumers
        // can compile this with a Kotlin compiler down to one minor version before this.
        // Pick the oldest not deprecated version.
        languageVersion.set(KotlinVersion.KOTLIN_2_2)
        // Prevents using newer APIs from the Kotlin standard library. So consumers can run this library with a Kotlin
        // standard library down to this version.
        // Pick the oldest not deprecated version.
        apiVersion.set(KotlinVersion.KOTLIN_2_2)
        // Depend on the oldest compatible Kotlin standard libraries (by default the Kotlin plugin coerces it to the one
        // matching its version). So consumers can safely use this or any later Kotlin standard library.
        // Pick the first release matching the versions above.
        // Note: when changing, also update coroutines dependency version (as this does not set that).
        coreLibrariesVersion = "2.2.0"
    }
}

val sourcesJar by tasks.registering(Jar::class) {
    group = "build"
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

dependencies {
    // Note: compileOnly so consumers do not depend on the coroutines library unless they manually
    // add it.
    compileOnly(libs.kotlin.coroutines.core.compat)

    api(project(":objectbox-java"))
}

// Note: common settings applied by objectbox.publishing-conventions plugin
val publicationObjectboxKotlin = "objectboxKotlin"
publishing {
    publications {
        create<MavenPublication>(publicationObjectboxKotlin) {
            artifactId = "objectbox-kotlin"

            from(components["java"])
            artifact(sourcesJar)
            // Note: the javadocJar task is created by the objectbox.dokka-conventions plugin
            artifact(tasks.named("javadocJar"))

            pom {
                name.set("ObjectBox Kotlin API")
                description.set("ObjectBox is a fast NoSQL database for Objects")
                packaging = "jar"
            }
        }
    }
}

signing {
    sign(publishing.publications[publicationObjectboxKotlin])
}
