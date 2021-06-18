package com.ryandens.javaagent

import java.io.File
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.distribution.DistributionContainer
import org.gradle.api.distribution.plugins.DistributionPlugin

/**
 * Gradle plugin for configuration a project that uses the [org.gradle.api.distribution.plugins.DistributionPlugin] to
 * include javaagents in the main distribution
 */
class JavaagentDistributionPlugin : Plugin<Project>, JavaagentPlugin {

    private val destinationDirectory = "lib"

    private lateinit var javaagentPathProvider: () -> String

    override fun dependentProjectPlugins(): Collection<Class<out Plugin<Project>>> {
        return setOf(DistributionPlugin::class.java)
    }

    override fun applyAfterJavaagentSetup(
        project: Project,
        javaagentConfiguration: NamedDomainObjectProvider<Configuration>
    ) {
        // Create a function to build the relative path to the javaagent inside the distribution to defer evaluation of javaagentConfiguration.get()
        javaagentPathProvider = {
            "$destinationDirectory/${File(javaagentConfiguration.get().asPath).name}"
        }
        project.extensions.getByType(DistributionContainer::class.java).named(DistributionPlugin.MAIN_DISTRIBUTION_NAME)
            .configure { distribution ->
                distribution.contents { copy ->
                    copy.from(javaagentConfiguration) {
                        it.into(destinationDirectory)
                    }
                }
            }
    }

    /**
     * Returns the name relative path of the javaagent JAR inside the distribution. Must not be invoked until project
     * configuration time
     */
    fun getAgentRelativeDistributionPath(): String {
        return javaagentPathProvider.invoke()
    }
}
