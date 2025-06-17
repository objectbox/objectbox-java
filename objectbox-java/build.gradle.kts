import kotlin.io.path.appendText
import kotlin.io.path.readText
import kotlin.io.path.writeText

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

    javadocTool.set(javaToolchains.javadocToolFor {
        // Note: the style changes only work if using JDK 10+, 17 is the LTS release used to publish this
        languageVersion.set(JavaLanguageVersion.of(17))
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
    setDestinationDir(javadocForWebDir.get().asFile)

    title = "ObjectBox Java ${project.version} API"
    (options as StandardJavadocDocletOptions).apply {
        overview = "$projectDir/src/web/overview.html"
        bottom = "Available under the Apache License, Version 2.0 - <i>Copyright &#169; 2017-2025 <a href=\"https://objectbox.io/\">ObjectBox Ltd</a>. All Rights Reserved.</i>"
    }

    doLast {
        // Note: frequently check the vanilla stylesheet.css if values still match.
        val stylesheetPath = "$destinationDir/stylesheet.css"

        // Adjust the CSS stylesheet

        // Change some color values
        // The stylesheet file should be megabytes at most, so read it as a whole
        val stylesheetFile = kotlin.io.path.Path(stylesheetPath)
        val originalContent = stylesheetFile.readText()
        val replacedContent = originalContent
            .replace("#4D7A97", "#17A6A6") // Primary background
            .replace("#F8981D", "#7DDC7D") // "Active" background
            .replace("#bb7a2a", "#E61955") // Hover
        stylesheetFile.writeText(replacedContent)
        // Note: in CSS stylesheets the last added rule wins, so append to default stylesheet.
        // Make code blocks scroll instead of stick out on small width
        stylesheetFile.appendText("pre {\n    overflow-x: auto;\n}\n")

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
