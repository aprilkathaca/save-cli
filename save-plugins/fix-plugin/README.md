## Save fix plugin
Plugin that runs provided executable on the initial file with a test source code and compares its output with an expected result.
Please note, that it is important for test resources to have specific postfixes. By the default test file it should be `Test`
, for the file with expected result - it should be `Expected`.

// FixMe: say some words about the configuration of postfixes

## Configuration
Assuming you want to run your tool on input file path/to/example1/ExampleTest.kt and compare with /path/to/example1/ExampleExpected.kt,
and you have directory structure like this
```bash
build.gradle.kts
save.properties
src/main/kotlin
src/test/resources
| save.toml
| path
  | to
    | example1
      | ExampleTest.kt
      | ExampleExpected.kt
      | Example2Test.kt
      | Example2Expected.kt
    | example2
    ...
```
you will need the following SAVE configuration:

`save.properties`:
```properties
rootDir=src/test/resources
reports=plain,json
reportsDir=build/reports/save
mode=parallel
language=kotlin
```

`save.toml`:
```toml
[general]
execCmd="./ktlint -R diktat-0.4.2.jar"
description = "My suite description"
suiteName = "DocsCheck"

[fix]
execFlags="-F"
batchSize = 1 # (default value)
batchSeparator = ", " # (default value)
resourceNameTestSuffix = "Test" # (default value)
resourceNameExpectedSuffix = "Expected" #(default value)
```

When executed from project root (where `save.propertes` is located), SAVE will cd to `rootDir` and discover all pairs of files. It will then execute `$execCmd $testFile`. `batchSize` it controls how many files execCmd will process at a time. (since we specified
`batchSize = 1`, it will provide inputs one by one) and compare its stdout (as per `output` option) with respecting `$expectedFile`.  `batchSeparator` is separator for filenames in `execCmd` if `batchSize > 1`.
`resourceNameTestSuffix` must include suffix name of the test file. `resourceNameExpectedSuffix` must include suffix name of the expected file.
