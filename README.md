**Tested on Java LTS versions from <!--property:java-runtime.min-version-->8<!--/property--> to <!--property:java-runtime.max-version-->21<!--/property-->.**

**Tested on Gradle versions from <!--property:gradle-api.min-version-->6.1<!--/property--> to <!--property:gradle-api.max-version-->8.8-rc-1<!--/property-->.**

# `name.remal.jacoco-to-cobertura` plugin

[![configuration cache: supported](https://img.shields.io/static/v1?label=configuration%20cache&message=supported&color=success)](https://docs.gradle.org/current/userguide/configuration_cache.html)

For every [`JacocoReport`](https://docs.gradle.org/current/javadoc/org/gradle/testing/jacoco/tasks/JacocoReport.html) task,
this plugin creates a task that converts Jacoco XML report to Cobertura format.

It can be useful for [GitLab test coverage visualization](https://docs.gitlab.com/ee/ci/testing/test_coverage_visualization.html).

The name of created task is `<jacoco task name>ToCobertura`. Examples:

* `jacocoTestReport` -> `jacocoTestReportToCobertura`
* `jacocoIntegrationTestReport` -> `jacocoIntegrationTestReportToCobertura`

By default, `*ToCobertura` tasks create XML files in the same directory where Jacoco XML report is.
Prefix `cobertura-` is added to the file name.
Example:

* `build/reports/jacoco/test/jacocoTestReport.xml` -> `build/reports/jacoco/test/cobertura-jacocoTestReport.xml`

Created tasks are not executed automatically. It can be done by this script:

```groovy
tasks.withType(JacocoReport).configureEach { finalizedBy("${name}ToCobertura") }
```

## `allJacocoReportToCobertura` task

This plugin creates `allJacocoReportToCobertura` task which executes all `*ToCobertura` tasks.
