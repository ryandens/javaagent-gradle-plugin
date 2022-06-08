# Javaagent Gradle Plugin

![Build](https://github.com/ryandens/javaagent-gradle-plugin/workflows/Validate/badge.svg?branch=main)
[![Javaagent Application Plugin](https://img.shields.io/maven-metadata/v/https/plugins.gradle.org/m2/com.ryandens/plugin/maven-metadata.xml.svg?label=Javaagent%20Application%20Plugin)](https://plugins.gradle.org/plugin/com.ryandens.javaagent-application)
[![Javaagent Jib Plugin](https://img.shields.io/maven-metadata/v/https/plugins.gradle.org/m2/com.ryandens/plugin/maven-metadata.xml.svg?label=Javaagent%20Jib%20Plugin)](https://plugins.gradle.org/plugin/com.ryandens.javaagent-jib)
[![Javaagent OTel Modification Plugin](https://img.shields.io/maven-metadata/v/https/plugins.gradle.org/m2/com.ryandens/plugin/maven-metadata.xml.svg?label=Javaagent%20OTel%20Modification%20Plugin)](https://plugins.gradle.org/plugin/com.ryandens.javaagent-otel-modification)


A set of vendor-agnostic Gradle plugins to ease building Java applications that leverage instrumentation agents in 
development and/or in production 

## Javaagent Application Plugin usage

This Gradle plugin tightly integrates with the [Gradle application plugin](https://docs.gradle.org/current/userguide/application_plugin.html) 
to make instrumenting your application build by Gradle easy! Simply register the `javaagent-application` plugin and 
then specify the javaagent you would like to attach in the dependencies block

```kotlin
plugins {
    application
    id("com.ryandens.javaagent-application") version "0.3.2"
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

### Jib integration

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
    id("com.ryandens.javaagent-jib") version "0.3.2"
}

jib.container {
    mainClass = "yourMainClassName"
}

dependencies {
    javaagent("io.opentelemetry.javaagent:opentelemetry-javaagent:1.11.1")
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
  id("com.ryandens.javaagent-otel-modification") version "0.3.2"
  id("com.ryandens.javaagent-application") version "0.3.2"
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
