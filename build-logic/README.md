# Build logic project

This project provides common build logic and is included as a composite build in the root settings file (as it provides
a plugin, in the `pluginManagement` block instead of before the usual include statements):

```kotlin
pluginManagement {
    includeBuild("build-logic")
}
```

## Convention Plugins

- [objectbox.publishing-conventions](src/main/kotlin/objectbox.publishing-conventions.gradle.kts): adds GitLab repo,
  configures common Maven POM values, configures signing of artifacts

## Related Gradle documentation

- https://docs.gradle.org/current/userguide/composite_builds.html#included_plugin_builds
- https://docs.gradle.org/current/userguide/best_practices_structuring_builds.html#use_convention_plugins
- https://github.com/gradle/gradle/tree/master/platforms/documentation/docs/src/snippets/best-practices/useConventionPlugins-do
