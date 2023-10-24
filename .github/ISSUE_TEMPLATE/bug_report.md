---
name: Bug report
about: You found a bug in ObjectBox causing an application to crash or throw an exception, or something does not work right.
title: ''
labels: 'bug'
assignees: ''

---

<!--

If you are looking for support, please check out our documentation:
- https://docs.objectbox.io
- https://docs.objectbox.io/faq
- https://docs.objectbox.io/troubleshooting

-->

### Is there an existing issue?

- [ ] I have searched [existing issues](https://github.com/objectbox/objectbox-java/issues)

### Build info

- ObjectBox version: [e.g. 3.7.0]
- OS: [e.g. Android 14 | Ubuntu 22.04 | Windows 11 ]
- Device/ABI/architecture: [e.g. Galaxy S23 | arm64-v8a | x86-64 ]

### Steps to reproduce

_TODO Tell us exactly how to reproduce the problem._

1. ...
2. ...
3. ...

### Expected behavior

_TODO Tell us what you expect to happen._

### Actual behavior

_TODO Tell us what actually happens._


### Code

_TODO Add a code example to help us reproduce your problem._

<!--

Please provide a minimal code example.

Things you maybe should also include:
- the entity class
- the Gradle build script

You can also create a public GitHub repository and link to it below.

Please do not upload screenshots of text, use code blocks like below instead.

Add any other context about the problem:
- Is there anything special about your app?
- May transactions or multi-threading play a role?
- Did you find any workarounds to prevent the issue?

-->

<details><summary>Code</summary>

```java
[Paste your code here]
```

</details>

### Logs, stack traces

_TODO Add relevant logs, a stack trace or crash report._

<!-- 

- For build issues, use `--stacktrace` to run the failing Gradle task. E.g. ./gradlew build --stacktrace
- For runtime errors, check log output (e.g. Logcat on Android).
  - For Java/Kotlin exceptions include the full stack trace.
  - For native crashes on Android, include Logcat output starting from the *** *** *** line. Note that crash reporting tools like Crashlytics may hide this line.
  - For native crashes on JVM, include the hs_err_pidXXXX.log file.
  - Also check logs before the error. ObjectBox logs are tagged with e.g. Box.

-->

<details><summary>Logs</summary>

```console
[Paste your logs here]
```

</details>
