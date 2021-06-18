package com.ryandens.javaagent

import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration

/**
 * Mixin for [org.gradle.api.Plugin]s that rely on the [JavaagentBasePlugin]
 */
interface JavaagentPlugin : Plugin<Project> {

    /**
     * Initial setup for any plugin that wants to configure a javaagent for a project, followed by delegating to the
     * [applyAfterJavaagentSetup] implementation provided by the plugin implementing this interface
     */
    override fun apply(project: Project) {
        // apply base plugin
        project.pluginManager.apply(JavaagentBasePlugin::class.java)
        // get configuration
        val javaagentConfiguration = project.configurations.named(JavaagentBasePlugin.CONFIGURATION_NAME)
        applyAfterJavaagentSetup(project, javaagentConfiguration)
    }

    fun applyAfterJavaagentSetup(project: Project, javaagentConfiguration: NamedDomainObjectProvider<Configuration>)
}
