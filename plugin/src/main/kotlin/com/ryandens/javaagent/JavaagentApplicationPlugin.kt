package com.ryandens.javaagent

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Convenience plugin to apply for those who want to take advantage of both the [JavaagentApplicationRunPlugin] and the
 * [JavaagentApplicationDistributionPlugin].
 */
class JavaagentApplicationPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.pluginManager.apply(JavaagentApplicationDistributionPlugin::class.java)
        target.pluginManager.apply(JavaagentApplicationRunPlugin::class.java)
    }
}
