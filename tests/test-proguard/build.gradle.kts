plugins {
    id("java-library")
}

tasks.withType<JavaCompile> {
    // Note: use release flag instead of sourceCompatibility and targetCompatibility to ensure only JDK 8 API is used.
    // https://docs.gradle.org/current/userguide/building_java_projects.html#sec:java_cross_compilation
    options.release.set(8)
}

repositories {
    // Native lib might be deployed only in internal repo
    if (project.hasProperty("gitlabUrl")) {
        val gitlabUrl = project.property("gitlabUrl")
        maven {
            url = uri("$gitlabUrl/api/v4/groups/objectbox/-/packages/maven")
            name = "GitLab"
            credentials(HttpHeaderCredentials::class) {
                name = project.findProperty("gitlabPrivateTokenName")?.toString() ?: "Private-Token"
                value = project.property("gitlabPrivateToken").toString()
            }
            authentication {
                create<HttpHeaderAuthentication>("header")
            }
            println("Dependencies: added GitLab repository $url")
        }
    } else {
        println("Dependencies: GitLab repository not added. To resolve dependencies from the GitLab Package Repository, set gitlabUrl and gitlabPrivateToken.")
    }
}

val obxJniLibVersion: String by rootProject.extra

val junitVersion: String by rootProject.extra

dependencies {
    implementation(project(":objectbox-java"))

    // Check flag to use locally compiled version to avoid dependency cycles
    if (!project.hasProperty("noObjectBoxTestDepencies")
        || project.property("noObjectBoxTestDepencies") == false) {
        println("Using $obxJniLibVersion")
        implementation(obxJniLibVersion)
    } else {
        println("Did NOT add native dependency")
    }

    testImplementation("junit:junit:$junitVersion")
}
