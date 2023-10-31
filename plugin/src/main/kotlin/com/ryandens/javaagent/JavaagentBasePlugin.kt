package com.ryandens.javaagent

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Creates javaagent configuration for usage in all plugins produced by this project
 */
class JavaagentBasePlugin : Plugin<Project> {
    companion object {
        const val CONFIGURATION_NAME = "javaagent"
    }

    override fun apply(project: Project) {
        // Register configuration
        project.configurations.register(CONFIGURATION_NAME) {
            // we expect javaagents to come as shaded JARs
            it.isTransitive = false
        }
    }
}
