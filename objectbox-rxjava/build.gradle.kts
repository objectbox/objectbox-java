plugins {
    id("java-library")
    id("objectbox.publishing-conventions")
}

// Note: use release flag instead of sourceCompatibility and targetCompatibility to ensure only JDK 8 API is used.
// https://docs.gradle.org/current/userguide/building_java_projects.html#sec:java_cross_compilation
tasks.withType<JavaCompile> {
    options.release.set(8)
}

dependencies {
    api(project(":objectbox-java"))
    api(libs.rxjava2)

    testImplementation(libs.junit)
    testImplementation(libs.mockito)
}

val javadocJar by tasks.registering(Jar::class) {
    dependsOn(tasks.javadoc)
    archiveClassifier.set("javadoc")
    from("build/docs/javadoc")
}

val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

// Note: common settings applied by objectbox.publishing-conventions plugin
val publicationObjectboxRxjava = "objectboxRxjava"
publishing {
    publications {
        create<MavenPublication>(publicationObjectboxRxjava) {
            artifactId = "objectbox-rxjava"

            from(components["java"])
            artifact(sourcesJar)
            artifact(javadocJar)

            pom {
                name.set("ObjectBox RxJava API")
                description.set("RxJava extension for ObjectBox")
                packaging = "jar"
            }
        }
    }
}

signing {
    sign(publishing.publications[publicationObjectboxRxjava])
}
