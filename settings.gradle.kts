pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral() // For dokka plugin
    }
}

// While this is an incubating API, it is the recommended way of declaring repositories:
// https://docs.gradle.org/current/userguide/best_practices_dependencies.html#set_up_repositories_in_settings
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()

        // Internal ObjectBox repo to get snapshot versions of dependencies
        val gitlabUrl: String? by settings
        if (gitlabUrl != null) {
            maven {
                url = uri("$gitlabUrl/api/v4/groups/objectbox/-/packages/maven")
                name = "GitLab"
                val gitlabPrivateTokenName: String? by settings
                val gitlabPrivateToken: String? by settings
                credentials(HttpHeaderCredentials::class) {
                    name = gitlabPrivateTokenName ?: "Private-Token"
                    value = gitlabPrivateToken
                }
                authentication {
                    create<HttpHeaderAuthentication>("header")
                }
                println("Dependencies: added GitLab repository at $url")
            }
        } else {
            println("Dependencies: GitLab repository not added. To resolve dependencies from the GitLab Package registry, set Gradle properties gitlabUrl and gitlabPrivateToken.")
        }
    }
}

plugins {
    // Supports resolving toolchains for JVM projects
    // https://docs.gradle.org/8.0/userguide/toolchains.html#sub:download_repositories
    id("org.gradle.toolchains.foojay-resolver-convention") version ("0.4.0")
}

include(":objectbox-java-api")
include(":objectbox-java")
include(":objectbox-kotlin")
include(":objectbox-rxjava")
include(":objectbox-rxjava3")

include(":tests:objectbox-java-test")
include(":tests:test-proguard")
