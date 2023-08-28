plugins {
    id("java-library")
    id("objectbox-publish")
}

// Note: use release flag instead of sourceCompatibility and targetCompatibility to ensure only JDK 8 API is used.
// https://docs.gradle.org/current/userguide/building_java_projects.html#sec:java_cross_compilation
tasks.withType(JavaCompile).configureEach {
    options.release.set(8)
}

dependencies {
    api project(':objectbox-java')
    api 'io.reactivex.rxjava2:rxjava:2.2.21'

    testImplementation "junit:junit:$junitVersion"
    testImplementation "org.mockito:mockito-core:$mockitoVersion"
}

tasks.register('javadocJar', Jar) {
    dependsOn javadoc
    archiveClassifier.set('javadoc')
    from 'build/docs/javadoc'
}

tasks.register('sourcesJar', Jar) {
    archiveClassifier.set('sources')
    from sourceSets.main.allSource
}

// Set project-specific properties.
publishing.publications {
    mavenJava(MavenPublication) {
        from components.java
        artifact sourcesJar
        artifact javadocJar
        pom {
            name = 'ObjectBox RxJava API'
            description = 'RxJava extension for ObjectBox'
        }
    }
}
