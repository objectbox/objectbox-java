# Repository Guidelines

## Project Structure & Module Organization
- Multi-module Gradle build. Core modules live at the repo root: `objectbox-java-api`, `objectbox-java`, `objectbox-kotlin`, `objectbox-rxjava`, `objectbox-rxjava3`.
- Test modules live under `tests/`, notably `tests/objectbox-java-test` and `tests/test-proguard`.
- Source follows standard Gradle layout per module (e.g., `src/main/java`, `src/main/kotlin`, `src/test/java`).
- CI and helper scripts live in `ci/` and `scripts/`; build outputs go to `build/`.

## Build, Test, and Development Commands
- `./gradlew clean build` — full build across modules.
- `./gradlew :objectbox-java:build` — build a single module.
- `./gradlew :tests:objectbox-java-test:test` — run the main test suite.
- `./gradlew -version` — verify Gradle/JDK setup.
- `./scripts/test-with-asan.sh --stacktrace clean build` — CI-style run with ASAN (requires native deps; see `Jenkinsfile`).

## Coding Style & Naming Conventions
- No repo-wide formatter config is present; follow existing Java/Kotlin style in the touched files and keep changes minimal.
- Use 4-space indentation and standard Java/Kotlin naming conventions; prefer package naming under `io.objectbox`.
- New tests in `tests/objectbox-java-test` should name methods as `{attribute}_{queryCondition}_{expectation}` (see `tests/README.md`).

## Testing Guidelines
- Tests are JUnit-based (see `tests/objectbox-java-test/build.gradle.kts`).
- Place new tests under `tests/objectbox-java-test/src/test/java` (or Kotlin equivalents).
- Legacy tests may still use the `test*` prefix; new tests should follow the naming convention above.

## Key Classes & Hotspots
- Core API: `io.objectbox.BoxStore`, `io.objectbox.Box`, and `io.objectbox.Transaction` (see `tests/objectbox-java-test/src/test/java/io/objectbox/BoxStoreTest.java` and `tests/objectbox-java-test/src/test/java/io/objectbox/BoxTest.java`).
- Queries: `io.objectbox.query.Query`, `QueryBuilder`, and `QueryCondition` (see `tests/objectbox-java-test/src/test/java/io/objectbox/query/QueryTest.java` and `tests/objectbox-java-test/src/test/java/io/objectbox/query/QueryTest2.java`).
- Relations: `io.objectbox.relation.ToOne` and `ToMany` (see `tests/objectbox-java-test/src/test/java/io/objectbox/relation/RelationTest.java`).
- Sync: `io.objectbox.sync.SyncClient`, `SyncServer`, and `SyncCredentials` (see `tests/objectbox-java-test/src/test/java/io/objectbox/sync/SyncTest.java`).

## Commit & Pull Request Guidelines
- Recent history uses a short area prefix, e.g., `QueryTest: use new API...` or `TargetIdProperty: improve...`. Follow this pattern for new commits.
- PRs should include a concise summary, rationale, and the exact test commands run.
- If changes affect native or CI behavior, call that out explicitly (CI runs JDK 8 and 16 and uses ASAN).
