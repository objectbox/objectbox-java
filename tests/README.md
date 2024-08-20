# Tests for `objectbox-java`

## Naming convention for tests

All new tests which will be added to the `tests/objectbox-java-test` module must have the names of their methods in the
following format: `{attribute}_{queryCondition}_{expectation}`

For ex. `date_lessAndGreater_works`

Note: due to historic reasons (JUnit 3) existing test methods may be named differently (with the `test` prefix).
