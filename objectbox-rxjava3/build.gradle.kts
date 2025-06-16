buildscript {
    ext.javadocDir = file("$buildDir/docs/javadoc")
}

plugins {
    id("java-library")
    id("kotlin")
    id("org.jetbrains.dokka")
    id("objectbox-publish")
}

// Note: use release flag instead of sourceCompatibility and targetCompatibility to ensure only JDK 8 API is used.
// https://docs.gradle.org/current/userguide/building_java_projects.html#sec:java_cross_compilation
tasks.withType(JavaCompile).configureEach {
    options.release.set(8)
}

// Produce Java 8 byte code, would default to Java 6.
tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile).configureEach {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

tasks.named("dokkaHtml") {
    outputDirectory.set(javadocDir)

    dokkaSourceSets {
        configureEach {
            // Fix "Can't find node by signature": have to manually point to dependencies.
            // https://github.com/Kotlin/dokka/wiki/faq#dokka-complains-about-cant-find-node-by-signature-
            externalDocumentationLink {
                // Point to web javadoc for objectbox-java packages.
                url.set(new URL("https://objectbox.io/docfiles/java/current/"))
                // Note: Using JDK 9+ package-list is now called element-list.
                packageListUrl.set(new URL("https://objectbox.io/docfiles/java/current/element-list"))
            }
        }
    }
}

dependencies {
    api project(':objectbox-java')
    api 'io.reactivex.rxjava3:rxjava:3.0.11'
    compileOnly "org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion"

    testImplementation "org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion"
    testImplementation "junit:junit:$junitVersion"
    testImplementation "org.mockito:mockito-core:$mockitoVersion"
}

tasks.register('javadocJar', Jar) {
    dependsOn tasks.named("dokkaHtml")
    group = 'build'
    archiveClassifier.set('javadoc')
    from "$javadocDir"
}

tasks.register('sourcesJar', Jar) {
    group = 'build'
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
            name = 'ObjectBox RxJava 3 API'
            description = 'RxJava 3 extensions for ObjectBox'
        }
    }
}
