plugins {
    id("maven-publish")
    id("signing")
}

// Make javadoc task errors not break the build, some are in third-party code.
if (JavaVersion.current().isJava8Compatible) {
    tasks.withType<Javadoc> {
        isFailOnError = false
    }
}

publishing {
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
        // Note: Sonatype repo created by publish-plugin, see root build.gradle.kts.
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

signing {
    if (hasSigningProperties()) {
        val signingKey = File(project.property("signingKeyFile").toString()).readText()
        useInMemoryPgpKeys(
            project.property("signingKeyId").toString(),
            signingKey,
            project.property("signingPassword").toString()
        )
        sign(publishing.publications["mavenJava"])
    } else {
        println("Signing information missing/incomplete for ${project.name}")
    }
}

fun hasSigningProperties(): Boolean {
    return (project.hasProperty("signingKeyId")
            && project.hasProperty("signingKeyFile")
            && project.hasProperty("signingPassword"))
}
