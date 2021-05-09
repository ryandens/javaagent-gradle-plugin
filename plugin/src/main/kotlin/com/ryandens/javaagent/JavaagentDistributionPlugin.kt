package com.ryandens.javaagent

import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.distribution.DistributionContainer
import org.gradle.api.distribution.plugins.DistributionPlugin
import java.io.File

/**
 * Gradle plugin for configuration a project that uses the [org.gradle.api.distribution.plugins.DistributionPlugin] to
 * include javaagents in the main distribution
 */
class JavaagentDistributionPlugin : Plugin<Project>, JavaagentPlugin {

    private val destinationDirectory = "lib"

    private lateinit var javaagentPathProvider: () -> String

    override fun apply(project: Project) {
        // ideally, we would get the DistributionPlugin ID from the class itself, but this information is not exposed
        failIfPluginNotApplied(project, "distribution")
        val javaagentConfiguration = setupAndGetJavaagentConfiguration(project)
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