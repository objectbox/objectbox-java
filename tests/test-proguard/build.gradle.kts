plugins {
    id("java-library")
}

tasks.withType<JavaCompile> {
    // Note: use release flag instead of sourceCompatibility and targetCompatibility to ensure only JDK 8 API is used.
    // https://docs.gradle.org/current/userguide/building_java_projects.html#sec:java_cross_compilation
    options.release.set(8)
}

val versionDatabaseLibraryJvm: String by rootProject.extra

val junitVersion: String by rootProject.extra

dependencies {
    implementation(project(":objectbox-java"))
    implementation("io.objectbox:objectbox-linux:$versionDatabaseLibraryJvm")
    implementation("io.objectbox:objectbox-macos:$versionDatabaseLibraryJvm")
    implementation("io.objectbox:objectbox-windows:$versionDatabaseLibraryJvm")

    testImplementation("junit:junit:$junitVersion")
}
