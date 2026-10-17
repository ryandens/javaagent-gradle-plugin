package com.ryandens.javaagent

import com.ryandens.javaagent.utils.JavaagentVersionUtil
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.testing.Test

class JavaagentTestPlugin :
    Plugin<Project>,
    JavaagentPlugin {
    companion object {
        const val CONFIGURATION_NAME = "testJavaagent"
    }

    override fun applyAfterJavaagentSetup(
        project: Project,
        javaagentConfiguration: NamedDomainObjectProvider<Configuration>,
    ) {
        // Register configuration
        val javaagentTestConfiguration =
            project.configurations.register(CONFIGURATION_NAME) {
                // we expect javaagents to come as shaded JARs
                it.isTransitive = false
                it.extendsFrom(javaagentConfiguration.get())
            }

        configureVersionInference(project, javaagentTestConfiguration, "testRuntimeClasspath")

        val extension = project.extensions.create("javaagentTest", JavaagentTestExtension::class.java)

        val enabled = calculateEnabled(project, extension)

        project.tasks.withType(Test::class.java).configureEach { test ->
            if (enabled.get()) {
                // Each test task resolves agents independently, since JVM targets can use different versions.
                val taskConfiguration =
                    if (test.name == "test") {
                        javaagentTestConfiguration
                    } else {
                        JavaagentBasePlugin
                            .targetConfiguration(project, "${test.name}Javaagent", javaagentTestConfiguration)
                            .also { configureVersionInference(project, it, "${test.name}RuntimeClasspath") }
                    }
                val optionsByFilePath = AgentOptionsResolver.optionsByFilePath(taskConfiguration)
                test.inputs.property("agentOptions", optionsByFilePath)
                test.inputs.files(taskConfiguration)
                JavaForkOptionsConfigurer.configureJavaForkOptions(
                    test,
                    taskConfiguration.map { it.files },
                    optionsByFilePath,
                )
            }
        }
    }

    private fun configureVersionInference(
        project: Project,
        configuration: NamedDomainObjectProvider<Configuration>,
        runtimeConfigurationName: String,
    ) {
        configuration.configure {
            it.resolutionStrategy.eachDependency { dependency ->
                if (dependency.requested.version.isNullOrBlank()) {
                    dependency.useVersion(
                        JavaagentVersionUtil.versionFromDependencyAndConfiguration(
                            dependency.requested,
                            project.configurations.named(runtimeConfigurationName),
                        ),
                    )
                }
            }
        }
    }

    private fun calculateEnabled(
        project: Project,
        extension: JavaagentTestExtension,
    ): Provider<Boolean> {
        val javaagentTestEnabled = project.findProperty("javaagentTestEnabled")
        val enabled =
            extension.enabled.map {
                if (javaagentTestEnabled != null) {
                    (javaagentTestEnabled as String).toBoolean()
                } else {
                    it
                }
            }
        return enabled
    }
}
