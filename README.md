# Javaagent Gradle Plugin

![Build](https://github.com/ryandens/javaagent-gradle-plugin/workflows/Validate/badge.svg?branch=main)
[![Javaagent Application Plugin](https://img.shields.io/maven-metadata/v/https/plugins.gradle.org/m2/com.ryandens/plugin/maven-metadata.xml.svg?label=Javaagent%20Application%20Plugin)](https://plugins.gradle.org/plugin/com.ryandens.javaagent-application)
[![Javaagent Jib Plugin](https://img.shields.io/maven-metadata/v/https/plugins.gradle.org/m2/com.ryandens/plugin/maven-metadata.xml.svg?label=Javaagent%20Jib%20Plugin)](https://plugins.gradle.org/plugin/com.ryandens.javaagent-jib)

A set of vendor-agnostic Gradle plugins to ease building Java applications that leverage instrumentation agents in 
development and/or in production 

## Javaagent Application Plugin usage

This Gradle plugin tightly integrates with the [Gradle application plugin](https://docs.gradle.org/current/userguide/application_plugin.html) 
to make instrumenting your application build by Gradle easy! Simply register the `javaagent-application` plugin and 
then specify the javaagent you would like to attach in the dependencies block

```kotlin
plugins {
    application
    id("com.ryandens.javaagent-application") version "0.2.1"
}

application {
    mainClass.set("yourMainClassName")
}

dependencies {
    javaagent("io.opentelemetry.javaagent:opentelemetry-javaagent:1.6.0:all")
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
    id("com.google.cloud.tools.jib") version "3.1.1"
    id("com.ryandens.javaagent-jib") version "0.2.1"
}

jib.container {
    mainClass = "yourMainClassName"
}

dependencies {
    javaagent("io.opentelemetry.javaagent:opentelemetry-javaagent:1.6.0:all")
}
```
