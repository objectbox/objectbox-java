buildscript {
    // Typically, only edit those two:
    val objectboxVersionNumber = "3.3.2" // without "-SNAPSHOT", e.g. "2.5.0" or "2.4.0-RC"
    val objectboxVersionRelease =
        false  // set to true for releasing to ignore versionPostFix to avoid e.g. "-dev" versions

    // version post fix: "-<value>" or "" if not defined; e.g. used by CI to pass in branch name
    val versionPostFixValue = project.findProperty("versionPostFix")
    val versionPostFix = if (versionPostFixValue != null) "-$versionPostFixValue" else ""
    val obxJavaVersion by extra(objectboxVersionNumber + (if (objectboxVersionRelease) "" else "$versionPostFix-SNAPSHOT"))

    // Native library version for tests
    // Be careful to diverge here; easy to forget and hard to find JNI problems
    val nativeVersion = objectboxVersionNumber + (if (objectboxVersionRelease) "" else "-dev-SNAPSHOT")
    val osName = System.getProperty("os.name").toLowerCase()
    val objectboxPlatform = when {
        osName.contains("linux") -> "linux"
        osName.contains("windows") -> "windows"
        osName.contains("mac") -> "macos"
        else -> "unsupported"
    }
    val obxJniLibVersion by extra("io.objectbox:objectbox-$objectboxPlatform:$nativeVersion")

    val essentialsVersion by extra("3.1.0")
    val juniVersion by extra("4.13.2")
    val mockitoVersion by extra("3.8.0")
    val kotlinVersion by extra("1.7.0")
    val coroutinesVersion by extra("1.6.2")
    val dokkaVersion by extra("1.6.10")

    println("version=$obxJavaVersion")
    println("objectboxNativeDependency=$obxJniLibVersion")

    repositories {
        mavenCentral()
        maven {
            url = uri("https://plugins.gradle.org/m2/")
        }
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
        classpath("org.jetbrains.dokka:dokka-gradle-plugin:$dokkaVersion")
        // https://github.com/spotbugs/spotbugs-gradle-plugin/releases
        classpath("gradle.plugin.com.github.spotbugs.snom:spotbugs-gradle-plugin:4.7.0")
        classpath("io.github.gradle-nexus:publish-plugin:1.1.0")
    }
}

allprojects {
    group = "io.objectbox"
    val obxJavaVersion: String by rootProject.extra
    version = obxJavaVersion

    repositories {
        mavenCentral()
    }

    configurations.all {
        // Projects are using snapshot dependencies that may update more often than 24 hours.
        resolutionStrategy {
            cacheChangingModulesFor(0, "seconds")
        }
    }
}

// Make javadoc task errors not break the build, some are in third-party code.
if (JavaVersion.current().isJava8Compatible) {
    allprojects {
        tasks.withType<Javadoc> {
            isFailOnError = false
        }
    }
}

val projectNamesToPublish = listOf(
    "objectbox-java-api",
    "objectbox-java",
    "objectbox-kotlin",
    "objectbox-rxjava",
    "objectbox-rxjava3"
)

fun hasSigningProperties(): Boolean {
    return (project.hasProperty("signingKeyId")
            && project.hasProperty("signingKeyFile")
            && project.hasProperty("signingPassword"))
}

configure(subprojects.filter { projectNamesToPublish.contains(it.name) }) {
    apply(plugin = "maven-publish")
    apply(plugin = "signing")

    configure<PublishingExtension> {
        repositories {
            maven {
                name = "GitLab"
                if (project.hasProperty("gitlabUrl") && project.hasProperty("gitlabPrivateToken")) {
                    // "https://gitlab.example.com/api/v4/projects/<PROJECT_ID>/packages/maven"
                    val gitlabUrl = project.property("gitlabUrl")
                    url = uri("$gitlabUrl/api/v4/projects/14/packages/maven")
                    println("GitLab repository set to $url.")

                    credentials(HttpHeaderCredentials::class) {
                        name = project.findProperty("gitlabTokenName")?.toString() ?: "Private-Token"
                        value = project.property("gitlabPrivateToken").toString()
                    }
                    authentication {
                        create<HttpHeaderAuthentication>("header")
                    }
                } else {
                    println("WARNING: Can not publish to GitLab: gitlabUrl or gitlabPrivateToken not set.")
                }
            }
            // Note: Sonatype repo created by publish-plugin.
        }

        publications {
            create<MavenPublication>("mavenJava") {
                // Note: Projects set additional specific properties.
                pom {
                    packaging = "jar"
                    url.set("https://objectbox.io")
                    licenses {
                        license {
                            name.set("The Apache Software License, Version 2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                            distribution.set("repo")
                        }
                    }
                    developers {
                        developer {
                            id.set("ObjectBox")
                            name.set("ObjectBox")
                        }
                    }
                    issueManagement {
                        system.set("GitHub Issues")
                        url.set("https://github.com/objectbox/objectbox-java/issues")
                    }
                    organization {
                        name.set("ObjectBox Ltd.")
                        url.set("https://objectbox.io")
                    }
                    scm {
                        connection.set("scm:git@github.com:objectbox/objectbox-java.git")
                        developerConnection.set("scm:git@github.com:objectbox/objectbox-java.git")
                        url.set("https://github.com/objectbox/objectbox-java")
                    }
                }
            }
        }
    }

    configure<SigningExtension> {
        if (hasSigningProperties()) {
            val signingKey = File(project.property("signingKeyFile").toString()).readText()
            useInMemoryPgpKeys(
                project.property("signingKeyId").toString(),
                signingKey,
                project.property("signingPassword").toString()
            )
            sign((extensions.getByName("publishing") as PublishingExtension).publications["mavenJava"])
        } else {
            println("Signing information missing/incomplete for ${project.name}")
        }
    }
}

tasks.wrapper {
    distributionType = Wrapper.DistributionType.ALL
}

// Plugin to publish to Central https://github.com/gradle-nexus/publish-plugin/
// This plugin ensures a separate, named staging repo is created for each build when publishing.
apply(plugin = "io.github.gradle-nexus.publish-plugin")
configure<io.github.gradlenexus.publishplugin.NexusPublishExtension> {
    repositories {
        sonatype {
            if (project.hasProperty("sonatypeUsername") && project.hasProperty("sonatypePassword")) {
                println("nexusPublishing credentials supplied.")
                username.set(project.property("sonatypeUsername").toString())
                password.set(project.property("sonatypePassword").toString())
            } else {
                println("nexusPublishing credentials NOT supplied.")
            }
        }
    }
    transitionCheckOptions {  // Maven Central may become very, very slow in extreme situations
        maxRetries.set(900)  // with default delay of 10s, that's 150 minutes total; default is 60 (10 minutes)
    }
}
