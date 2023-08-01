plugins {
    // Supports resolving toolchains for JVM projects
    // https://docs.gradle.org/8.0/userguide/toolchains.html#sub:download_repositories
    id("org.gradle.toolchains.foojay-resolver-convention") version("0.4.0")
}

include(":objectbox-java-api")
include(":objectbox-java")
include(":objectbox-kotlin")
include(":objectbox-rxjava")
include(":objectbox-rxjava3")

include(":tests:objectbox-java-test")
include(":tests:test-proguard")
