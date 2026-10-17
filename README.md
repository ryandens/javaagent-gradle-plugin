# Javaagent Gradle Plugin

[![Build](https://github.com/ryandens/javaagent-gradle-plugin/actions/workflows/gradle.yml/badge.svg?branch=main)](https://github.com/ryandens/javaagent-gradle-plugin/actions/workflows/gradle.yml)

[![Javaagent Application Plugin](https://img.shields.io/maven-metadata/v/https/plugins.gradle.org/m2/com.ryandens/plugin/maven-metadata.xml.svg?label=Javaagent%20Application%20Plugin)](https://plugins.gradle.org/plugin/com.ryandens.javaagent-application)
[![Javaagent Jib Plugin](https://img.shields.io/maven-metadata/v/https/plugins.gradle.org/m2/com.ryandens/plugin/maven-metadata.xml.svg?label=Javaagent%20Jib%20Plugin)](https://plugins.gradle.org/plugin/com.ryandens.javaagent-jib)
[![Javaagent Test Plugin](https://img.shields.io/maven-metadata/v/https/plugins.gradle.org/m2/com.ryandens/plugin/maven-metadata.xml.svg?label=Javaagent%20Test%20Plugin)](https://plugins.gradle.org/plugin/com.ryandens.javaagent-test)
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
    id("com.ryandens.javaagent-test") version "0.11.0"
}

dependencies {
    testImplementation(platform("org.mockito:mockito-bom:5.18.0"))
    testImplementation("org.mockito:mockito-core")
    testJavaagent("org.mockito:mockito-core")
}
```

> ℹ️ **Note:** The [Javaagent Test Plugin](#java-test-task-integration) automatically resolves dependency versions for javaagents dependencies that did not request one from the test runtime classpath. 

## Android JVM test integration

The `com.ryandens.javaagent-test-android` plugin (available in the next release) attaches agents to
host-side JVM tests in Android application and library projects. Apply it instead of
`com.ryandens.javaagent-test` in Android modules:

```kotlin
plugins {
    id("com.android.application") // or com.android.library, using your project's AGP version
    id("com.ryandens.javaagent-test-android") version "<plugin-version>"
}

dependencies {
    testImplementation(platform("org.mockito:mockito-bom:5.18.0"))
    testImplementation("org.mockito:mockito-core")
    testJavaagent("org.mockito:mockito-core")
}
```

Agents declared in `javaagent` are inherited; options on `testJavaagent` can override shared options.
Versionless agents are resolved using each host test variant's runtime classpath, including its
flavor/build-type dependencies and BOM constraints. Custom flavors and build types are discovered
through Android's public Variant API. Either plugin order works, and neither the Java nor Android
plugin is applied implicitly. Device/emulator instrumentation tests and Android app packaging are
unaffected.

Set `javaagentTest { enabled.set(false) }` or `-PjavaagentTestEnabled=false` to disable attachment.
The project property takes precedence. Disabled agents are not resolved.

Compatibility is tested with AGP 8.13.2 / Gradle 9.5 and AGP 9.4.1 / Gradle 9.8.1, including
configuration-cache reuse and Isolated Projects. AGP 8 requires Gradle below 9.6 because it uses
an internal Gradle API removed in 9.6. To run this repository's Android functional tests, set
`ANDROID_HOME` (or `ANDROID_SDK_ROOT`) and install `platforms;android-36` and `build-tools;36.0.0`.
Run the compatibility suite with `./gradlew :android:functionalTest -PandroidTestAgpVersion=8.13.2`.

## Application Plugin integration

This Gradle plugin tightly integrates with the [Gradle application plugin](https://docs.gradle.org/current/userguide/application_plugin.html) 
to make instrumenting your application build by Gradle easy! Simply register the `javaagent-application` plugin and 
then specify the javaagent you would like to attach in the dependencies block

```kotlin
plugins {
    application
    id("com.ryandens.javaagent-application") version "0.11.0"
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

## Passing options to javaagents

Some javaagents accept options via the `-javaagent:<jar>=<options>` form of the flag (see the
[`java.lang.instrument` documentation](https://docs.oracle.com/en/java/javase/21/docs/api/java.instrument/java/lang/instrument/package-summary.html#starting-an-agent-from-the-command-line-interface-heading)).
For example, the [Prometheus JMX Exporter](https://github.com/prometheus/jmx_exporter) agent is configured as
`-javaagent:jmx_prometheus_javaagent.jar=12345:config.yaml`.

Agent options belong to dependency declarations. Each plugin provides a configuration for its own
execution or packaging context:

| Plugin | Dependency configuration | Used by |
| --- | --- | --- |
| `com.ryandens.javaagent-application-run` | `applicationRunJavaagent` | Local application `run` |
| `com.ryandens.javaagent-test` | `testJavaagent` | Java `test` |
| `com.ryandens.javaagent-application-distribution` | `applicationDistributionJavaagent` | `installDist`, `distTar`, and `distZip` launchers |
| `com.ryandens.javaagent-jib` | `jibJavaagent` | Jib container image entrypoints |

The `com.ryandens.javaagent-application` convenience plugin applies both application plugins and
provides both `applicationRunJavaagent` and `applicationDistributionJavaagent`. Apply only the plugins
needed for your project; each registers its own configuration.

In Kotlin DSL, import `agentOptions` and set it on each dependency:

```kotlin
import com.ryandens.javaagent.agentOptions

plugins {
    application
    id("com.ryandens.javaagent-application") version "0.11.0"
    id("com.ryandens.javaagent-test") version "0.11.0"
    id("com.google.cloud.tools.jib") version "3.5.3"
    id("com.ryandens.javaagent-jib") version "0.11.0"
}

dependencies {
    applicationRunJavaagent("io.prometheus.jmx:jmx_prometheus_javaagent:0.20.0") {
        agentOptions = "12345:config/local.yaml"
    }
    testJavaagent("io.prometheus.jmx:jmx_prometheus_javaagent:0.20.0") {
        agentOptions = "12346:config/test.yaml"
    }
    applicationDistributionJavaagent("io.prometheus.jmx:jmx_prometheus_javaagent:0.20.0") {
        agentOptions = "12345:config/distribution.yaml"
    }
    jibJavaagent("io.prometheus.jmx:jmx_prometheus_javaagent:0.20.0") {
        agentOptions = "12345:/etc/agent/container.yaml"
    }
}
```

Agents declared only in a plugin's configuration are included only in that context. Project
dependencies work the same way, for example `applicationRunJavaagent(project(":my-agent")) { agentOptions = "..." }`.
Tar, zip, and installed distributions share options because they package the same generated launchers.

The existing `javaagent` dependency configuration is shared: declarations there are inherited by each
plugin-specific configuration. Use it when the same agent and options should apply to every enabled
context. Options explicitly set on a plugin-specific declaration override inherited options for that
resolved agent. This follows the configuration hierarchy: descendant declarations override ancestors.
An empty string overrides inherited options with an empty options suffix. Conflicting options in the
same configuration or in unrelated parent configurations fail unless a descendant supplies an override.
Agents without configured options are attached without an options suffix.

Every downstream configuration can override options inherited from `javaagent`. For example, to
override shared options for tests:

```kotlin
dependencies {
    javaagent("com.example:my-agent:1.0") {
        agentOptions = "shared-options"
    }
    testJavaagent("com.example:my-agent:1.0") {
        agentOptions = "test-options"
    }
}
```

Application runs, distributions, and Jib images use `shared-options`; tests use `test-options`.
Declare overrides in `applicationRunJavaagent`, `applicationDistributionJavaagent`, or `jibJavaagent`
in the same way to customize those contexts. The same rule applies to custom configurations extending
`javaagent`.
Omit `agentOptions` from the `javaagent` declaration to attach the agent everywhere with
options only in tests. With only the shared declaration, every enabled context uses its options.
The base plugin registers `javaagent`; the context plugins inherit from it and attach the agents.

### Migrating shared resolution rules

Plugin-specific configurations resolve their own dependencies. Gradle's `extendsFrom` shares dependency
declarations, constraints, and exclusions; it does not inherit resolution strategies or configuration
attributes. Previously, application run, distributions, and Jib resolved `javaagent` directly. If your
build customized `configurations.javaagent.resolutionStrategy`, move those rules to all relevant agent
configurations when upgrading:

```kotlin
val agentConfigurations = setOf(
    "javaagent", "applicationRunJavaagent", "testJavaagent", "applicationDistributionJavaagent", "jibJavaagent",
)

configurations.matching { it.name in agentConfigurations }.configureEach {
    resolutionStrategy.dependencySubstitution {
        substitute(module("com.example:my-agent")).using(project(":my-agent"))
    }
}
```

Use the same pattern for forced versions, `eachDependency` rules, or configuration attributes.
`configureEach` also covers configurations registered later by another agent plugin. To customize only
one context, configure that plugin's configuration directly, for example `configurations.applicationRunJavaagent`.

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
    id("com.ryandens.javaagent-jib") version "0.11.0"
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
    id("com.ryandens.javaagent-test") version "0.11.0"
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
  id("com.ryandens.javaagent-otel-modification") version "0.11.0"
  id("com.ryandens.javaagent-application") version "0.11.0"
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
