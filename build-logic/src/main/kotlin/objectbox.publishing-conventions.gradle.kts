// This convention plugin expects some Gradle project properties to be set
// (to set as environment variable prefix with ORG_GRADLE_PROJECT_):
// https://docs.gradle.org/current/userguide/build_environment.html#sec:project_properties
//
// To publish artifacts to the internal GitLab repo set:
// - gitlabUrl
// - gitlabPublishToken: a token with permission to publish to the GitLab Package Repository
// - gitlabPublishTokenName: optional, if set used instead of "Private-Token". Use for CI to specify e.g. "Job-Token".
//
// To sign artifacts using an ASCII encoded PGP key given via a file set:
// - signingKeyFile
// - signingKeyId
// - signingPassword

// Gradle properties
val propertyGitlabUrl = "gitlabUrl"
val propertyGitlabPublishToken = "gitlabPublishToken"
val propertyPublishTokenName = "gitlabPublishTokenName"
val propertySigningKeyFile = "signingKeyFile"
val propertySigningKeyId = "signingKeyId"
val propertySigningPassword = "signingPassword"

plugins {
    id("maven-publish")
    id("signing")
}

publishing {
    repositories {
        maven {
            name = "GitLab"
            if (project.hasProperty(propertyGitlabUrl) && project.hasProperty(propertyGitlabPublishToken)) {
                // "https://gitlab.example.com/api/v4/projects/<PROJECT_ID>/packages/maven"
                val gitlabUrl = project.property(propertyGitlabUrl)
                url = uri("$gitlabUrl/api/v4/projects/14/packages/maven")
                credentials(HttpHeaderCredentials::class) {
                    name = project.findProperty(propertyPublishTokenName)?.toString() ?: "Private-Token"
                    value = project.property(propertyGitlabPublishToken).toString()
                }
                authentication {
                    create<HttpHeaderAuthentication>("header")
                }
                println("Publishing: configured GitLab repository $url")
            } else {
                println("Publishing: GitLab repository not configured")
            }
        }
        // Note: Sonatype repo created by publish-plugin, see root build.gradle.kts.
    }

    publications {
        // Common settings for all Maven publications
        withType<MavenPublication> {
            // Note: Projects set additional specific properties.
            pom {
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
        // Sign using an ASCII-armored key read from a file
        // https://docs.gradle.org/current/userguide/signing_plugin.html#using_in_memory_ascii_armored_openpgp_subkeys
        val signingKey = File(project.property(propertySigningKeyFile).toString()).readText()
        useInMemoryPgpKeys(
            project.property(propertySigningKeyId).toString(),
            signingKey,
            project.property(propertySigningPassword).toString()
        )
        println("Publishing: configured signing with key file")
    } else {
        isRequired = false // Don't run sign tasks
        println("Publishing: signing not configured")
    }
}

private fun hasSigningProperties(): Boolean {
    return (project.hasProperty(propertySigningKeyId)
            && project.hasProperty(propertySigningKeyFile)
            && project.hasProperty(propertySigningPassword))
}
