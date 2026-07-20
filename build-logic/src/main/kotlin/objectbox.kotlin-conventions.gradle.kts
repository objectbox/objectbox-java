// This convention plugin configures the Kotlin plugin to guarantee that consumers of the applied to
// project can use the oldest compatible Kotlin compiler and standard libraries.
// https://kotlinlang.org/docs/kotlin-evolution-principles.html#compatibility-tools

import org.jetbrains.kotlin.gradle.dsl.HasConfigurableKotlinCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinBaseExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

// Use new Kolin 2.2 configuration API that works for JVM and Android Kotlin plugins
// https://kotlinlang.org/docs/whatsnew21.html#new-api-for-kotlin-gradle-plugin-extensions
configure<KotlinBaseExtension> {
    if (this is HasConfigurableKotlinCompilerOptions<*>) {
        with(compilerOptions) {
            if (this is KotlinJvmCompilerOptions) {
                // Produce Java 8 byte code
                jvmTarget.set(JvmTarget.JVM_1_8)
            }
            // Prevents using newer language features, sets this as the Kotlin version in produced metadata. So consumers
            // can compile this with a Kotlin compiler down to one minor version before this.
            // Pick the oldest not deprecated version.
            languageVersion.set(KotlinVersion.KOTLIN_2_2)
            // Prevents using newer APIs from the Kotlin standard library. So consumers can run this library with a Kotlin
            // standard library down to this version.
            // Pick the oldest not deprecated version.
            apiVersion.set(KotlinVersion.KOTLIN_2_2)
            // Depend on the oldest compatible Kotlin standard libraries (by default the Kotlin plugin coerces it to the one
            // matching its version). So consumers can safely use this or any later Kotlin standard library.
            // Pick the first release matching the versions above.
            // Note: when changing, also update coroutines dependency version (as this does not set that).
            coreLibrariesVersion = "2.2.0"
        }
    }
}
