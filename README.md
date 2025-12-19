# Javaagent Gradle Plugin

[![Build](https://github.com/ryandens/javaagent-gradle-plugin/actions/workflows/gradle.yml/badge.svg?branch=main)](https://github.com/ryandens/javaagent-gradle-plugin/actions/workflows/gradle.yml)

[![Javaagent Application Plugin](https://img.shields.io/maven-metadata/v/https/plugins.gradle.org/m2/com.ryandens/plugin/maven-metadata.xml.svg?label=Javaagent%20Application%20Plugin)](https://plugins.gradle.org/plugin/com.ryandens.javaagent-application)
[![Javaagent Jib Plugin](https://img.shields.io/maven-metadata/v/https/plugins.gradle.org/m2/com.ryandens/plugin/maven-metadata.xml.svg?label=Javaagent%20Jib%20Plugin)](https://plugins.gradle.org/plugin/com.ryandens.javaagent-jib)
[![Javaagent Test Plugin](https://img.shields.io/maven-metadata/v/https/plugins.gradle.org/m2/com.ryandens/plugin/maven-metadata.xml.svg?label=Javaagent%20Test%20Plugin)](https://plugins.gradle.org/plugin/com.ryandens.javaagent-test)
[![Javaagent Test Android Plugin](https://img.shields.io/maven-metadata/v/https/plugins.gradle.org/m2/com.ryandens/plugin/maven-metadata.xml.svg?label=Javaagent%20Test%20Android%20Plugin)](https://plugins.gradle.org/plugin/com.ryandens.javaagent-test-android)
[![Javaagent OTel Modification Plugin](https://img.shields.io/maven-metadata/v/https/plugins.gradle.org/m2/com.ryandens/plugin/maven-metadata.xml.svg?label=Javaagent%20OTel%20Modification%20Plugin)](https://plugins.gradle.org/plugin/com.ryandens.javaagent-otel-modification)


A set of vendor-agnostic Gradle plugins to ease building Java applications that leverage instrumentation agents in 
development and/or in production 

> The work on this software project is in no way associated with my employer nor with the role I'm having at my employer. Any requests for changes will be decided upon exclusively by myself based on my personal preferences. I maintain this project as much or as little as my spare time permits.

## Mockito Example

The Java community is quickly preparing for upcoming JDK restrictions that will improve the security and stability of the java community. One of these restrictions is the removal of the ability to self-attach javaagents.

You may have come across this plugin because you encountered the following message in your build:
> Mockito is currently self-attaching to enable the inline-mock-maker. This will no longer work in future releases of the JDK. Please add Mockito as an agent to your build as described in Mockito's documentation: https://javadoc.io/doc/org.mockito/mockito-core/latest/org.mockito/org/mockito/Mockito.html#0.3

This plugin enables you to quickly and easily resolve these constraints by adding the Mockito agent to your build as a javaagent, while maintaining compatability with the Gradle concepts such as up to date checks, test task caching, and configuration caching.


```kotlin
plugins {
    java
    id("com.ryandens.javaagent-application") version "0.9.1"
}

dependencies {
    testImplementation(platform("org.mockito:mockito-bom:5.18.0"))
    testImplementation("org.mockito:mockito-core")
    testJavaagent("org.mockito:mockito-core")
}
```

> ℹ️ **Note:** The [Javaagent Test Plugin](#java-test-task-integration) automatically resolves dependency versions for javaagents dependencies that did not request one from the test runtime classpath. 

## Android Example

The most typical scenario when you'd need this plugin on Android is using Mockito in JUnit tests. 

Simply apply plugin `javaagent-test-android` and add `testJavaagent` to your JUnit test dependencies just like you'd do with the regular Mockito:

```kotlin
plugins {
    id("com.android.application")
    id("com.ryandens.javaagent-test-android")
}

dependencies {
    testImplementation(platform("org.mockito:mockito-bom:5.18.0"))
    testImplementation("org.mockito:mockito-core")
    testJavaagent("org.mockito:mockito-core")
    
    // Note that Android tests use Mockito-Android which does not require java agent
    androidTestImplementation("org.mockito:mockito-android:5.21.0")
    androidTestImplementation("org.mockito:mockito-junit-jupiter:5.21.0")
}
```

Plugin `javaagent-test-android` supports both Application and Library type of Android project.


## Application Plugin integration

This Gradle plugin tightly integrates with the [Gradle application plugin](https://docs.gradle.org/current/userguide/application_plugin.html) 
to make instrumenting your application build by Gradle easy! Simply register the `javaagent-application` plugin and 
then specify the javaagent you would like to attach in the dependencies block

```kotlin
plugins {
    application
    id("com.ryandens.javaagent-application") version "0.9.1"
}

application {
    mainClass.set("yourMainClassName")
}

dependencies {
    javaagent("io.opentelemetry.javaagent:opentelemetry-javaagent:1.11.1")
}
```

Now, when you run the ApplicationPlugin's `run` task, your application will be instrumented with the javaagent specified
by the `javaagent` Gradle configuration! In addition, the distributions created by the `distTar` and `distZip` 
ApplicationPlugin tasks include the javaagent JAR in the archive's dependency directory. Finally, the distribution 
start scripts have been modified to automatically attach the agent via the command line `-javaagent` flag.

## Jib integration

[Jib](https://github.com/GoogleContainerTools/jib) is a build tool for containerizing Java applications without a Docker
daemon. This project integrates with the [jib-gradle-plugin](https://github.com/GoogleContainerTools/jib/tree/master/jib-gradle-plugin)
via the [jib-extensions](https://github.com/GoogleContainerTools/jib-extensions) API to add the javaagent JAR as a new 
layer in the container image and modifies the entrypoint of the container to automatically attach the agent at runtime 
via the command line `-javaagent` flag. Simply register the `javaagent-jib` plugin and then specify the javaagent you 
would like to attach in the dependencies block.

```kotlin
plugins {
    java
    id("com.google.cloud.tools.jib") version "3.1.4"
    id("com.ryandens.javaagent-jib") version "0.9.1"
}

jib.container {
    mainClass = "yourMainClassName"
}

dependencies {
    javaagent("io.opentelemetry.javaagent:opentelemetry-javaagent:1.11.1")
}
```

## Java Test Task integration
The Gradle Java Plugin creates a test task that launches a forked JVM from the build for testing purposes. The 
`com.ryandens.javaagent-test` plugin automatically configures the jvm to be launched with the javaagents specified by 
the `javaagent` configuration attached. This enables the use of runtime analysis via javaagents for projects. Common
uses cases for this include collecting telemetry data from test runs or leveraging an Interactive Application Security
Testing (IAST) product.


```kotlin
plugins {
    java
    id("com.ryandens.javaagent-test") version "0.9.1"
}

tasks.named<Test>("test") {
  useJUnitPlatform()
}

dependencies {
    javaagent("io.opentelemetry.javaagent:opentelemetry-javaagent:1.11.1")  
    testJavaagent("com.ryandens.example:agent:1.0.0") // only attached to test task but ignored by other gradle plugins in this project.
}
```

### OpenTelemetry Integration
[OpenTelemetry](https://OpenTelemetry.io) is a set of tools that enable distributed tracing. 
[OpenTelemetry Instrumentation for Java](https://github.com/open-telemetry/opentelemetry-java-instrumentation/) is a 
project that is often consumed as a javaagent in order to instrument OSS libraries with distributed tracing logic. There
are many distributions of this agent by specific vendors. In addition, OpenTelemetry offers a number of extension points
that consumers of the libraries and vendors can take advantage of.

This integration strives to make it easier for consumers of OpenTelemetry distributions to modify their chosen distributions
with other extensions and instrumentation modules. This is desirable because OpenTelemetry provides a high-level API for
instrumentation which makes writing instrumentation modules using their API a lot more approachable for those unfamiliar
with bytecode manipulation. 

This integration can be used by applying the `javaagent-otel-modification` plugin, specifying the OTel distribution you would like to
extend and the libraries or projects you would like to extend it with. In addition, this plugin also applies the
[JavaagentBasePlugin](./plugin/src/main/kotlin/com/ryandens/javaagent/JavaagentBasePlugin.kt) and registers the outputted
extended OpenTelemetry agent as a project dependency with the `javaagent` configuration. This does effectively nothing
on its own, but allows for seamless integration with plugins that leverage the `javaagent` configuration (such as 
`javaagent-application` and `javaagent-jib`) so that the extended OpenTelemetry agent is configured is included in the 
desired application distribution or execution method.


```kotlin
plugins {
  application
  id("com.ryandens.javaagent-otel-modification") version "0.9.1"
  id("com.ryandens.javaagent-application") version "0.9.1"
}

dependencies {
  // the otel configuration is used to identify the desired distribution to modify
  otel("io.opentelemetry.javaagent:opentelemetry-javaagent:1.12.0")
  // the otelExtension configuration expects a shaded JAR that conforms to OTel's rules
  otelExtension("io.opentelemetry.contrib:opentelemetry-samplers:1.12.0-alpha")
  // the otelInstrumentation expects a project whose references to the OTel API have been relocated in order to match the agent's shaded class names. Note, this plugin will rename the files from .class -> .classdata 
  otelInstrumentation(project(":custom-instrumentation"))
}

application {
  // Define the main class for the application.
  mainClass.set("com.ryandens.javaaagent.example.App")
}

setOf(tasks.distTar, tasks.distZip).forEach {
  it.configure {
    dependsOn(tasks.extendedAgent)
  }
}
```
