package com.ryandens.javaagent

import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.testing.Test

class JavaagentTestPlugin :
    Plugin<Project>,
    JavaagentPlugin {
    companion object {
        const val CONFIGURATION_NAME = "testJavaagent"
    }

    override fun dependentProjectPlugins(): Collection<Class<out Plugin<Project>>> = setOf(JavaPlugin::class.java)

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

        val extension = project.extensions.create("javaagentTest", JavaagentTestExtension::class.java)

        val enabled = calculateEnabled(project, extension)

        // configure the run task to use the `javaagent` flag pointing to the dependency stored in the local Maven repository
        project.tasks.named(JavaPlugin.TEST_TASK_NAME, Test::class.java).configure {
            if (enabled.get()) {
                JavaForkOptionsConfigurer.configureJavaForkOptions(
                    it,
                    javaagentTestConfiguration.map { configuration ->
                        configuration.files
                    },
                )
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
