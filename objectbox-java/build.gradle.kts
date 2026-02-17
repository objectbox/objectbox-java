plugins {
    id("java-library")
    id("objectbox-publish")
    id("com.github.spotbugs")
}

// Note: use release flag instead of sourceCompatibility and targetCompatibility to ensure only JDK 8 API is used.
// https://docs.gradle.org/current/userguide/building_java_projects.html#sec:java_cross_compilation
tasks.withType<JavaCompile> {
    options.release.set(8)
}

val javadocForWebDir = layout.buildDirectory.dir("docs/web-api-docs")
val essentialsVersion: String by rootProject.extra

dependencies {
    api(project(":objectbox-java-api"))
    implementation("org.greenrobot:essentials:$essentialsVersion")
    api("com.google.code.findbugs:jsr305:3.0.2")

    // https://github.com/spotbugs/spotbugs/blob/master/CHANGELOG.md
    compileOnly("com.github.spotbugs:spotbugs-annotations:4.8.6")
}

spotbugs {
    ignoreFailures.set(true)
    showStackTraces.set(true)
    excludeFilter.set(file("spotbugs-exclude.xml"))
}

tasks.spotbugsMain {
    reports.create("html") {
        required.set(true)
    }
}

// Note: used for the Maven javadoc artifact, a separate task is used to build API docs to publish online
tasks.javadoc {
    // Internal Java APIs
    exclude("**/io/objectbox/Cursor.java")
    exclude("**/io/objectbox/InternalAccess.java")
    exclude("**/io/objectbox/KeyValueCursor.java")
    exclude("**/io/objectbox/ModelBuilder.java")
    exclude("**/io/objectbox/Properties.java")
    exclude("**/io/objectbox/Transaction.java")
    exclude("**/io/objectbox/ideasonly/**")
    exclude("**/io/objectbox/internal/**")
    exclude("**/io/objectbox/query/InternalAccess.java")
    exclude("**/io/objectbox/reactive/DataPublisherUtils.java")
    exclude("**/io/objectbox/reactive/WeakDataObserver.java")
    exclude("**/io/objectbox/sync/server/ClusterPeerInfo.java")
    // Repackaged FlatBuffers distribution
    exclude("**/io/objectbox/flatbuffers/**")
    // FlatBuffers generated files only used internally (note: some are part of the public API)
    exclude("**/io/objectbox/model/**")
    exclude("**/io/objectbox/sync/Credentials.java")
    exclude("**/io/objectbox/sync/CredentialsType.java")
    exclude("**/io/objectbox/sync/server/ClusterPeerConfig.java")
    exclude("**/io/objectbox/sync/server/JwtConfig.java")
    exclude("**/io/objectbox/sync/server/SyncServerOptions.java")
}

// Note: use packageJavadocForWeb to get as ZIP.
val javadocForWeb by tasks.registering(Javadoc::class) {
    group = "documentation"
    description = "Builds Javadoc incl. objectbox-java-api classes with web tweaks."

    // Register used files as inputs so task is re-run if they change
    // https://docs.gradle.org/current/userguide/incremental_build.html
    val customOverview = layout.projectDirectory.file("src/web/overview.html")
    inputs.file(customOverview)
        .withPropertyName("customOverview")
        .withPathSensitivity(PathSensitivity.NONE)
    val customStylesheet = layout.projectDirectory.file("src/web/objectbox-stylesheet.css")
    inputs.file(customStylesheet)
        .withPropertyName("customStylesheet")
        .withPathSensitivity(PathSensitivity.NONE)

    javadocTool.set(javaToolchains.javadocToolFor {
        // Note: the style changes only work if using JDK 10+, 21 is the LTS release used to publish this
        languageVersion.set(JavaLanguageVersion.of(21))
    })

    val srcApi = project(":objectbox-java-api").file("src/main/java/")
    if (!srcApi.isDirectory) throw GradleException("Not a directory: $srcApi")
    // Hide internal API from javadoc artifact.
    val filteredSources = sourceSets.main.get().allJava.matching {
        // Internal Java APIs
        exclude("**/io/objectbox/Cursor.java")
        exclude("**/io/objectbox/InternalAccess.java")
        exclude("**/io/objectbox/KeyValueCursor.java")
        exclude("**/io/objectbox/ModelBuilder.java")
        exclude("**/io/objectbox/Properties.java")
        exclude("**/io/objectbox/Transaction.java")
        exclude("**/io/objectbox/ideasonly/**")
        exclude("**/io/objectbox/internal/**")
        exclude("**/io/objectbox/query/InternalAccess.java")
        exclude("**/io/objectbox/reactive/DataPublisherUtils.java")
        exclude("**/io/objectbox/reactive/WeakDataObserver.java")
        exclude("**/io/objectbox/sync/server/ClusterPeerInfo.java")
        // Repackaged FlatBuffers distribution
        exclude("**/io/objectbox/flatbuffers/**")
        // FlatBuffers generated files only used internally (note: some are part of the public API)
        exclude("**/io/objectbox/model/**")
        exclude("**/io/objectbox/sync/Credentials.java")
        exclude("**/io/objectbox/sync/CredentialsType.java")
        exclude("**/io/objectbox/sync/server/ClusterPeerConfig.java")
        exclude("**/io/objectbox/sync/server/JwtConfig.java")
        exclude("**/io/objectbox/sync/server/SyncServerOptions.java")
    }
    source = filteredSources + fileTree(srcApi)

    classpath = sourceSets.main.get().output + sourceSets.main.get().compileClasspath
    destinationDir = javadocForWebDir.get().asFile

    title = "ObjectBox Java ${project.version} API"
    (options as StandardJavadocDocletOptions).apply {
        overview = customOverview.toString()
        bottom = "Available under the Apache License, Version 2.0 - <i>Copyright &#169; 2017-2026 <a href=\"https://objectbox.io/\">ObjectBox Ltd</a>. All Rights Reserved.</i>"
        // Customize the default stylesheet https://docs.oracle.com/en/java/javase/21/javadoc/javadoc-css-themes.html
        // Note: the javadoc option is "--add-stylesheet", but addStringOption already ads a single dash ("-")
        addStringOption("-add-stylesheet", customStylesheet.toString())
    }

    doLast {
        println("Javadoc for web created at $destinationDir")
    }
}

tasks.register<Zip>("packageJavadocForWeb") {
    dependsOn(javadocForWeb)
    group = "documentation"
    description = "Packages Javadoc incl. objectbox-java-api classes with web tweaks as ZIP."

    archiveFileName.set("objectbox-java-web-api-docs.zip")
    val distDir = layout.buildDirectory.dir("dist")
    destinationDirectory.set(distDir)

    from(file(javadocForWebDir))

    doLast {
        println("Javadoc for web packaged to ${distDir.get().file("objectbox-java-web-api-docs.zip")}")
    }
}

val javadocJar by tasks.registering(Jar::class) {
    dependsOn(tasks.javadoc)
    archiveClassifier.set("javadoc")
    from("build/docs/javadoc")
}

val sourcesJar by tasks.registering(Jar::class) {
    from(sourceSets.main.get().allSource)
    archiveClassifier.set("sources")
}

// Set project-specific properties.
publishing {
    publications {
        getByName<MavenPublication>("mavenJava") {
            from(components["java"])
            artifact(sourcesJar)
            artifact(javadocJar)
            pom {
                name.set("ObjectBox Java (only)")
                description.set("ObjectBox is a fast NoSQL database for Objects")
            }
        }
    }
}
