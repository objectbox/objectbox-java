plugins {
    id("java-library")
    id("objectbox-publish")
    id("com.github.spotbugs")
}

// Note: use release flag instead of sourceCompatibility and targetCompatibility to ensure only JDK 8 API is used.
// https://docs.gradle.org/current/userguide/building_java_projects.html#sec:java_cross_compilation
tasks.withType(JavaCompile).configureEach {
    options.release.set(8)
}

ext {
    javadocForWebDir = "$buildDir/docs/web-api-docs"
}

dependencies {
    api project(':objectbox-java-api')
    implementation "org.greenrobot:essentials:$essentialsVersion"
    api 'com.google.code.findbugs:jsr305:3.0.2'

    // https://github.com/spotbugs/spotbugs/blob/master/CHANGELOG.md
    compileOnly 'com.github.spotbugs:spotbugs-annotations:4.7.3'
}

spotbugs {
    ignoreFailures = true
    showStackTraces = true
    excludeFilter = file("spotbugs-exclude.xml")
}

tasks.spotbugsMain {
    reports.create("html") {
        required.set(true)
    }
}

// Note: used for the Maven javadoc artifact, a separate task is used to build API docs to publish online
javadoc {
    // Internal Java APIs
    exclude("**/io/objectbox/Cursor.java")
    exclude("**/io/objectbox/KeyValueCursor.java")
    exclude("**/io/objectbox/ModelBuilder.java")
    exclude("**/io/objectbox/Properties.java")
    exclude("**/io/objectbox/Transaction.java")
    exclude("**/io/objectbox/ideasonly/**")
    exclude("**/io/objectbox/internal/**")
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
tasks.register('javadocForWeb', Javadoc) {
    group = 'documentation'
    description = 'Builds Javadoc incl. objectbox-java-api classes with web tweaks.'

    javadocTool = javaToolchains.javadocToolFor {
        // Note: the style changes only work if using JDK 10+, 17 is the LTS release used to publish this
        languageVersion = JavaLanguageVersion.of(17)
    }

    def srcApi = project(':objectbox-java-api').file('src/main/java/')
    if (!srcApi.directory) throw new GradleScriptException("Not a directory: ${srcApi}", null)
    // Hide internal API from javadoc artifact.
    def filteredSources = sourceSets.main.allJava.matching {
        // Internal Java APIs
        exclude("**/io/objectbox/Cursor.java")
        exclude("**/io/objectbox/KeyValueCursor.java")
        exclude("**/io/objectbox/ModelBuilder.java")
        exclude("**/io/objectbox/Properties.java")
        exclude("**/io/objectbox/Transaction.java")
        exclude("**/io/objectbox/ideasonly/**")
        exclude("**/io/objectbox/internal/**")
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
    source = filteredSources + srcApi

    classpath = sourceSets.main.output + sourceSets.main.compileClasspath
    destinationDir = file(javadocForWebDir)

    title = "ObjectBox Java ${version} API"
    options.overview = "$projectDir/src/web/overview.html"
    options.bottom = 'Available under the Apache License, Version 2.0 - <i>Copyright &#169; 2017-2025 <a href="https://objectbox.io/">ObjectBox Ltd</a>. All Rights Reserved.</i>'

    doLast {
        // Note: frequently check the vanilla stylesheet.css if values still match.
        def stylesheetPath = "$destinationDir/stylesheet.css"

        // Primary background
        ant.replace(file: stylesheetPath, token: "#4D7A97", value: "#17A6A6")

        // "Active" background
        ant.replace(file: stylesheetPath, token: "#F8981D", value: "#7DDC7D")

        // Hover
        ant.replace(file: stylesheetPath, token: "#bb7a2a", value: "#E61955")

        // Note: in CSS stylesheets the last added rule wins, so append to default stylesheet.
        // Code blocks
        file(stylesheetPath).append("pre {\nwhite-space: normal;\noverflow-x: auto;\n}\n")
        // Member summary tables
        file(stylesheetPath).append(".memberSummary {\noverflow: auto;\n}\n")
        // Descriptions and signatures
        file(stylesheetPath).append(".block {\n" +
                "    display:block;\n" +
                "    margin:3px 10px 2px 0px;\n" +
                "    color:#474747;\n" +
                "    overflow:auto;\n" +
                "}")

        println "Javadoc for web created at $destinationDir"
    }
}

tasks.register('packageJavadocForWeb', Zip) {
    dependsOn javadocForWeb
    group = 'documentation'
    description = 'Packages Javadoc incl. objectbox-java-api classes with web tweaks as ZIP.'

    archiveFileName = "objectbox-java-web-api-docs.zip"
    destinationDirectory = file("$buildDir/dist")

    from file(javadocForWebDir)

    doLast {
        println "Javadoc for web packaged to ${file("$buildDir/dist/objectbox-java-web-api-docs.zip")}"
    }
}

tasks.register('javadocJar', Jar) {
    dependsOn javadoc
    archiveClassifier.set('javadoc')
    from 'build/docs/javadoc'
}

tasks.register('sourcesJar', Jar) {
    from sourceSets.main.allSource
    archiveClassifier.set('sources')
}

// Set project-specific properties.
publishing.publications {
    mavenJava(MavenPublication) {
        from components.java
        artifact sourcesJar
        artifact javadocJar
        pom {
            name = 'ObjectBox Java (only)'
            description = 'ObjectBox is a fast NoSQL database for Objects'
        }
    }
}
