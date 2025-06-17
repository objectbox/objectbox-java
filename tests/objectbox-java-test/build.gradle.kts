import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("java-library")
    id("kotlin")
}

tasks.withType<JavaCompile> {
    // Note: use release flag instead of sourceCompatibility and targetCompatibility to ensure only JDK 8 API is used.
    // https://docs.gradle.org/current/userguide/building_java_projects.html#sec:java_cross_compilation
    options.release.set(8)
}

kotlin {
    compilerOptions {
        // Produce Java 8 byte code, would default to Java 6
        jvmTarget.set(JvmTarget.JVM_1_8)
    }
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

val coroutinesVersion: String by rootProject.extra
val essentialsVersion: String by rootProject.extra
val junitVersion: String by rootProject.extra

dependencies {
    implementation(project(":objectbox-java"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation(project(":objectbox-kotlin"))
    implementation("org.greenrobot:essentials:$essentialsVersion")

    // Check flag to use locally compiled version to avoid dependency cycles
    if (!project.hasProperty("noObjectBoxTestDepencies")
        || project.property("noObjectBoxTestDepencies") == false) {
        println("Using $obxJniLibVersion")
        implementation(obxJniLibVersion)
    } else {
        println("Did NOT add native dependency")
    }

    testImplementation("junit:junit:$junitVersion")
    // To test Coroutines
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
    // To test Kotlin Flow
    testImplementation("app.cash.turbine:turbine:0.5.2")
}

val testInMemory by tasks.registering(Test::class) {
    group = "verification"
    description = "Run unit tests with in-memory database"
    systemProperty("obx.inMemory", true)
}

// Run in-memory tests as part of regular check run
tasks.check {
    dependsOn(testInMemory)
}

tasks.withType<Test> {
    if (System.getenv("TEST_WITH_JAVA_X86") == "true") {
        // To run tests with 32-bit ObjectBox
        // Note: 32-bit JDK is only available on Windows
        val javaExecutablePath = System.getenv("JAVA_HOME_X86") + "\\bin\\java.exe"
        println("$name: will run tests with $javaExecutablePath")
        executable = javaExecutablePath
    } else if (System.getenv("TEST_JDK") != null) {
        // To run tests on a different JDK, uses Gradle toolchains API (https://docs.gradle.org/current/userguide/toolchains.html)
        val sdkVersionInt = System.getenv("TEST_JDK").toInt()
        println("$name: will run tests with JDK $sdkVersionInt")
        javaLauncher.set(javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(sdkVersionInt))
        })
    }

    // This is pretty useless now because it floods console with warnings about internal Java classes
    // However we might check from time to time, also with Java 9.
    // jvmArgs "-Xcheck:jni"

    filter {
        // Note: Tree API currently incubating on Linux only.
        if (!System.getProperty("os.name").lowercase().contains("linux")) {
            excludeTestsMatching("io.objectbox.tree.*")
        }
    }

    testLogging {
        exceptionFormat = TestExceptionFormat.FULL
        displayGranularity = 2
        // Note: this overwrites showStandardStreams = true, so set it by
        // adding the standard out/error events.
        events = setOf(
            TestLogEvent.STARTED,
            TestLogEvent.PASSED,
            TestLogEvent.STANDARD_OUT,
            TestLogEvent.STANDARD_ERROR
        )
    }
}