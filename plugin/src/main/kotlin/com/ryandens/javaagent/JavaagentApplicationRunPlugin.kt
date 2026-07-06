package com.ryandens.javaagent

import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.ApplicationPlugin
import org.gradle.api.tasks.JavaExec

/**
 * Configures a project that leverages the [ApplicationPlugin] to easily integrate a javaagent into its tasks and outputs.
 */
class JavaagentApplicationRunPlugin :
    Plugin<Project>,
    JavaagentPlugin {
    override fun dependentProjectPlugins(): Collection<String> = setOf("application")

    override fun applyAfterJavaagentSetup(
        project: Project,
        javaagentConfiguration: NamedDomainObjectProvider<Configuration>,
    ) {
        // configure the run task to use the `javaagent` flag pointing to the dependency stored in the local Maven repository
        project.tasks.named(ApplicationPlugin.TASK_RUN_NAME, JavaExec::class.java).configure {
            // The agent jars are passed to the JVM via a CommandLineArgumentProvider (see
            // JavaForkOptionsConfigurer), which resolves the configuration's files at execution time but is
            // not a tracked task input. Without an explicit dependency, Gradle establishes no ordering
            // between the run task and the tasks that build the agent jars, so under parallel execution the
            // run task can launch with -javaagent:<jar> before that jar has been produced, failing with
            // "Error opening zip file or JAR manifest missing".
            //
            // A Configuration is Buildable, so dependsOn adds the artifact-building task dependencies and
            // fixes the ordering. Unlike inputs.files(...), it does NOT register the agent jars as tracked
            // inputs, which avoids tripping Gradle's implicit-dependency validation for agent jars added to
            // the configuration without a declared builtBy (e.g. the otel example's extendedAgent jar). The
            // run task is never up-to-date, so input tracking would provide no benefit here anyway.
            it.dependsOn(javaagentConfiguration)
            JavaForkOptionsConfigurer.configureJavaForkOptions(it, javaagentConfiguration.map { configuration -> configuration.files })
        }
    }
}
