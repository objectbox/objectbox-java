rootProject.name = "build-logic"

// While this is an incubating API, it is the recommended way of declaring repositories:
// https://docs.gradle.org/current/userguide/best_practices_dependencies.html#set_up_repositories_in_settings
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
    // To avoid creating a separate version catalog in build-logic re-use the existing one
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}
