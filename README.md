> [!IMPORTANT]
> [JaCoCo Coverage Reports](https://docs.gitlab.com/ee/ci/testing/test_coverage_visualization/jacoco.html) feature is generally available in GitLab 17.6.
>
> If you use this plugin for GitLab's [Cobertura Coverage Reports](https://docs.gitlab.com/ee/ci/testing/test_coverage_visualization/cobertura.html), consider switching to JaCoCo Coverage Reports and removing this plugin from your build.

&nbsp;

**Tested on Java LTS versions from <!--property:java-runtime.min-version-->11<!--/property--> to <!--property:java-runtime.max-version-->21<!--/property-->.**

**Tested on Gradle versions from <!--property:gradle-api.min-version-->7.0<!--/property--> to <!--property:gradle-api.max-version-->9.0.0-rc-2<!--/property-->.**

# `name.remal.jacoco-to-cobertura` plugin

[![configuration cache: supported](https://img.shields.io/static/v1?label=configuration%20cache&message=supported&color=success)](https://docs.gradle.org/current/userguide/configuration_cache.html)

Usage:

<!--plugin-usage:name.remal.jacoco-to-cobertura-->
```groovy
plugins {
    id 'name.remal.jacoco-to-cobertura' version '2.0.1'
}
```
<!--/plugin-usage-->

&nbsp;

For every [`JacocoReport`](https://docs.gradle.org/current/javadoc/org/gradle/testing/jacoco/tasks/JacocoReport.html) task,
this plugin creates a task that converts Jacoco XML report to Cobertura format.
This new task is executed automatically after corresponding `JacocoReport` task (via `finalizedBy`).

It can be useful for [GitLab test coverage visualization](https://docs.gitlab.com/ee/ci/testing/test_coverage_visualization.html).

The name of created task is `<jacoco task name>ToCobertura`. Examples:

* `jacocoTestReport` -> `jacocoTestReportToCobertura`
* `jacocoIntegrationTestReport` -> `jacocoIntegrationTestReportToCobertura`

By default, `*ToCobertura` tasks create XML files in the same directory where Jacoco XML report is.
Prefix `cobertura-` is added to the file name.
Example:

* `build/reports/jacoco/test/jacocoTestReport.xml` -> `build/reports/jacoco/test/cobertura-jacocoTestReport.xml`

## `jacocoToCoberturaTask` extension for `JacocoReport` tasks

This plugin add `jacocoToCoberturaTask` extension to all `JacocoReport` tasks.
This extension has type `TaskProvider<JacocoToCobertura>`
and it's a provider of corresponding `*ToCobertura` task for the current `JacocoReport` task.

It can be used like this:

```groovy
tasks.withType(JacocoReport).configureEach {
  println jacocoToCoberturaTask.name // prints corresponding name of `*ToCobertura` task
}
```

# Migration guide

## Version 1.* to 2.*

The minimum Java version is 11 (from 8).
The minimum Gradle version is 7.0 (from 6.1).
