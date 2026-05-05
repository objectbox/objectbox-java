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
val gitlabUrl = providers.gradleProperty("gitlabUrl")
val gitlabPublishToken = providers.gradleProperty("gitlabPublishToken")
val gitlabPublishTokenName = providers.gradleProperty("gitlabPublishTokenName")
// The following properties are used for signing in CI using a key file
val signingKeyFile = providers.gradleProperty("signingKeyFile")
val signingKeyId = providers.gradleProperty("signingKeyId")
val signingPassword = providers.gradleProperty("signingPassword")

plugins {
    id("maven-publish")
    id("signing")
}

publishing {
    repositories {
        // If the applied to project has the required properties, configures the "GitLab" repository for publishing
        // (Note: always adding it, even without credentials, so it's possible to see the tasks created for it.)
        maven {
            name = "GitLab"
            if (gitlabUrl.isPresent && gitlabPublishToken.isPresent) {
                // "https://gitlab.example.com/api/v4/projects/<PROJECT_ID>/packages/maven"
                url = uri("${gitlabUrl.get()}/api/v4/projects/14/packages/maven")
                credentials(HttpHeaderCredentials::class) {
                    name = gitlabPublishTokenName.orNull ?: "Private-Token"
                    value = gitlabPublishToken.get()
                }
                authentication {
                    create<HttpHeaderAuthentication>("header")
                }
                println("Publishing: configured GitLab repository $url")
            } else {
                println("Publishing: GitLab repository NOT configured, see publishing-conventions plugin for required project properties")
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
    if (signingKeyFile.isPresent && signingKeyId.isPresent && signingPassword.isPresent) {
        // Sign using an ASCII-armored key read from a file
        // https://docs.gradle.org/current/userguide/signing_plugin.html#using_in_memory_ascii_armored_openpgp_subkeys
        val keyFilePath = signingKeyFile.get()
        val signingKey = File(keyFilePath).readText()
        useInMemoryPgpKeys(signingKeyId.get(), signingKey, signingPassword.get())
        println("Publishing: signing configured with key file $keyFilePath")
    } else {
        isRequired = false // Don't run sign tasks
        println("Publishing: signing NOT configured, see publishing-conventions plugin for required project properties")
    }
}
