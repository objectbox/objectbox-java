# ObjectBox Java API tests

Note: As the Gradle plugin and therefore code generation is not available, these tests use copies (partially customized
for testing) of generated ObjectBox helper classes like `MyObjectBox`, meta and cursor classes.

The following test projects exist:

- [objectbox-java-test](objectbox-java-test): basic tests of Java APIs using JVM libraries with Sync (client only)
  feature
- [test-proguard](test-proguard): despite its name, only a very basic create, read and delete test using the regular JVM
  library (so without Sync)

Extended tests exist in the internal Java integration test repository.

## Naming convention for tests

All new test methods should be named like: `{subject}_{operation}_{expectation}`

For ex. to test a date query condition: `date_lessAndGreater_works`

Note: due to historic reasons existing test methods (written for JUnit 3) may be named differently (with a `test`
prefix).
