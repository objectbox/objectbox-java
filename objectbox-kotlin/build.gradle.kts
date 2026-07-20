plugins {
    id("java-library")
    alias(libs.plugins.kotlin.jvm)
    id("objectbox.kotlin-conventions")
    id("objectbox.dokka-conventions")
    id("objectbox.publishing-conventions")
}

// Note: use release flag instead of sourceCompatibility and targetCompatibility to ensure only JDK 8 API is used.
// https://docs.gradle.org/current/userguide/building_java_projects.html#sec:java_cross_compilation
tasks.withType<JavaCompile> {
    options.release.set(8)
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
