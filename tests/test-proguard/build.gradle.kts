plugins {
    id("java-library")
}

tasks.withType<JavaCompile> {
    // Note: use release flag instead of sourceCompatibility and targetCompatibility to ensure only JDK 8 API is used.
    // https://docs.gradle.org/current/userguide/building_java_projects.html#sec:java_cross_compilation
    options.release.set(8)
}

val obxJniLibVersion: String by rootProject.extra

val junitVersion: String by rootProject.extra

dependencies {
    implementation(project(":objectbox-java"))

    // Check flag to use locally compiled version to avoid dependency cycles
    if (!project.hasProperty("noObjectBoxTestDepencies")
        || project.property("noObjectBoxTestDepencies") == false) {
        println("Using $obxJniLibVersion")
        implementation(obxJniLibVersion)
    } else {
        println("Did NOT add native dependency")
    }

    testImplementation("junit:junit:$junitVersion")
}
